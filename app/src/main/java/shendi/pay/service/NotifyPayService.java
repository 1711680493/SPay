package shendi.pay.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;
import java.util.Set;

import shendi.kit.time.TimeUtils;
import shendi.pay.Application;
import shendi.pay.R;
import shendi.pay.SLog;
import shendi.pay.util.ApiExecUtil;
import shendi.pay.util.ApiUtil;

/**
 * 通知支付服务.
 * 创建时间：2023/11/8
 * @author Shendi
 */
public class NotifyPayService extends NotificationListenerService {

    private static SLog log = SLog.getLogger(NotifyPayService.class.getName());

    public static final String NOTIFY_TITLE_DISPOSE_ERR = "监听处理失败";

    @Override
    public void onCreate() {
        super.onCreate();

        // 适配8.0及以上,创建渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(Application.NOTIFY_CHANNEL_ID_PAY, Application.NOTIFY_CHANNEL_ID_PAY, NotificationManager.IMPORTANCE_LOW));
        }

        // 前台服务
        startForeground(1, new NotificationCompat.Builder(this, Application.NOTIFY_CHANNEL_ID_PAY)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("SPay监听通知服务")
                .setContentText("安稳运行中...")
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .build());

        log.i("监听通知服务启动");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packName = sbn.getPackageName();
        Bundle extras = sbn.getNotification().extras;

        Object titleObj = extras.get(Notification.EXTRA_TITLE),
               contentObj = extras.get(Notification.EXTRA_TEXT);

        if (titleObj == null && contentObj == null) return;

        String title = titleObj == null ? "" : titleObj.toString(),
               content = contentObj == null ? "" : contentObj.toString();

        StringBuilder notifyStr = new StringBuilder();
        notifyStr.append("[").append(packName).append("]")
                .append(title).append(" : ")
                .append(content);

        log.i(notifyStr.toString());

        // 如果正在测试通知，那么将此通知广播至对应Activity
        if (Application.getInstance().isTestNotify) {
            // 发送广播
            Intent intent = new Intent(Application.RECEIVE_TEST);
            intent.putExtra("info", notifyStr.toString());
            sendBroadcast(intent);
        }

        // 如果是本APP的错误通知或者服务运行通知，那么不做处理，避免死循环
        if (getPackageName().equals(packName) && (NOTIFY_TITLE_DISPOSE_ERR.equals(title) || "安稳运行中...".equals(content))) {
            return;
        }

        // 检验是否为支付通知
        JSONObject info = Application.getInstance().getBasicInfo();
        String priKey = Application.getInstance().getBasicPriKey(null);
        if (info == null || priKey == null) {
            // 如果标题是当前标题，那么就不发送通知，否则会导致死循环
            Application.getInstance().sendNotify(NOTIFY_TITLE_DISPOSE_ERR, "没有配置基础信息或密钥,无法处理通知");
            return;
        }

        try {
            JSONObject result = getNotifyPayStr(packName, title, content, info);
            if (result == null) return;

            int amount = result.getIntValue("amount");
            String type = result.getString("type");

            ApiExecUtil.pay(title, content, result.getBooleanValue("isUp"), result.getString("purl"), priKey, amount, type);

            // 用于无障碍不重复
            Application.getInstance().setLastUp(packName, TimeUtils.getFormatTime("hour_minute").getString(System.currentTimeMillis()), String.valueOf(amount));
        } catch (Exception e) {
            e.printStackTrace();
            Application.getInstance().sendNotify(NOTIFY_TITLE_DISPOSE_ERR, "处理出错：" + e.getMessage());
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        log.i("监听通知已连接");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        log.i("监听通知已断开");
    }

    /**
     * 获取支付通知中的金额
     * @param packName  包名
     * @param title     通知标题
     * @param content   通知内容
     * @param infoObj   获取通知金额的规则字符串
     * @return JSON包含需要的信息amount,purl,type,isUp，空代表非支付通知
     */
    private JSONObject getNotifyPayStr(String packName, String title, String content, JSONObject infoObj) {
        JSONObject payObj = infoObj.getJSONObject("paystr");
        Set<String> poKeys = payObj.keySet();
        for (String poKey : poKeys) {
            // key是支付类型
            JSONObject poVal = payObj.getJSONObject(poKey);

            // 验证包名
            JSONArray poPackNames = poVal.getJSONArray("packName");
            for (Object popn : poPackNames) {
                if (packName.equals(popn)) {
                    // 根据列表获取金额
                    JSONArray list = poVal.getJSONArray("list");
                    if (list == null) {
                        Application.getInstance().sendNotify(NOTIFY_TITLE_DISPOSE_ERR, "配置的基础信息中，list为空, 类型=" + poKey);
                        continue;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        JSONObject item = list.getJSONObject(i);

                        // 校验是否可以匹配
                        String checkTitle = item.getString("checkTitle");
                        String checkContent = item.getString("checkContent");
                        if ((checkTitle != null && !title.contains(checkTitle))
                                || (checkContent != null && !content.contains(checkContent))) {
                            log.i("通知不匹配");
                            continue;
                        }

                        // 金额在标题还是在内容
                        boolean isTitle = item.getBooleanValue("isTitle");
                        String dataStr = isTitle ? title : content;

                        // start为前缀，end为后缀，空字符串代表无
                        String start = item.getString("start");
                        String end = item.getString("end");

                        if (!"".equals(start)) {
                            int startIndex = dataStr.indexOf(start);

                            if (startIndex == -1) continue;
                            else startIndex += start.length();

                            dataStr = dataStr.substring(startIndex);
                        }

                        if (!"".equals(end)) {
                            int endIndex = dataStr.indexOf(end);
                            if (endIndex == -1) continue;

                            dataStr = dataStr.substring(0, endIndex);
                        }

                        String amountStr = dataStr;

                        log.i("截取的金额为：" + amountStr);

                        // 能转换数字类型成功则代表是金额
                        int amount = 0;
                        try {
                            amount = new BigDecimal(amountStr).multiply(BigDecimal.valueOf(100)).intValue();

                            JSONObject result = new JSONObject(4);
                            result.put("amount", amount);
                            result.put("type", poKey);
                            result.put("purl", infoObj.getString("purl"));
                            // 是否上传
                            result.put("isUp", !poVal.containsKey("isUp") || poVal.getBooleanValue("isUp"));

                            return result;
                        } catch (Exception e) {
                            log.i("非支付通知，金额非数字: " + amountStr);
                            continue;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.i("监听通知服务销毁");
    }
}
