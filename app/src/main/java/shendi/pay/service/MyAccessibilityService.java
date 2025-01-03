package shendi.pay.service;

import android.accessibilityservice.AccessibilityService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.core.app.NotificationCompat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import shendi.kit.time.TimeUtils;
import shendi.pay.Application;
import shendi.pay.R;
import shendi.pay.SLog;
import shendi.pay.bean.ANodeInfo;
import shendi.pay.util.AccessibilityNodeUtil;
import shendi.pay.util.ApiExecUtil;
import shendi.pay.util.ApiUtil;

/**
 * 无障碍服务.
 * @author 砷碲
 */
public class MyAccessibilityService extends AccessibilityService {

    private static SLog log = SLog.getLogger(MyAccessibilityService.class.getName());

    /** 无障碍服务是否运行 */
    public static boolean isRun = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        // 适配8.0及以上,创建渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(Application.NOTIFY_CHANNEL_ID_ACCESSIBILITY, Application.NOTIFY_CHANNEL_ID_ACCESSIBILITY, NotificationManager.IMPORTANCE_DEFAULT));
        }

        // 前台服务
        startForeground(2, new NotificationCompat.Builder(this, Application.NOTIFY_CHANNEL_ID_ACCESSIBILITY)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("SPay无障碍服务")
                .setContentText("安稳运行中...")
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .build());

        isRun = true;

        log.d("无障碍服务开启，运行");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!Application.getInstance().getBasicAccessibility()) return;

//        log.d("accessibility event: [" + event.getEventType() + ", " + event.getAction() + "," + event.getPackageName() + ", " + event.getSource() + ", " + event.getParcelableData() + "]");

        if (event.getPackageName() == null) return;

        String packName = event.getPackageName().toString();

        if ("com.tencent.mm".equals(packName)) {
//            AccessibilityNodeUtil.printTree(getRootInActiveWindow());

            // 只有首页与微信支付消息页不会发送通知
            ANodeInfo rootNode = new ANodeInfo(getRootInActiveWindow());
            if (rootNode.node == null) return;

            // 主页与支付页都以FrameLayout开头
            if ("android.widget.FrameLayout".equals(rootNode.getClassName())) {
                try {
                    JSONObject info = Application.getInstance().getBasicInfo();
                    String priKey = Application.getInstance().getBasicPriKey(null);
                    if (info == null || priKey == null) {
                        // 如果标题是当前标题，那么就不发送通知，否则会导致死循环
                        Application.getInstance().sendNotify(NotifyPayService.NOTIFY_TITLE_DISPOSE_ERR, "没有配置基础信息或密钥,无法处理通知");
                        return;
                    }

                    JSONObject paystrVal = getPaystrValByPackName(packName, info);
                    if (paystrVal != null) {
                        ANodeInfo node1 = rootNode.getChildAndRecycle(0, "android.widget.LinearLayout")
                                .getChildAndRecycle(0, "android.widget.FrameLayout");

                        if (node1.node != null) {
                            String purl = info.getString("purl");

                            ANodeInfo mainNode = node1.getChild(0, "android.widget.FrameLayout");
                            ANodeInfo payMsgNode = node1.getChildAndRecycle(0, "android.widget.LinearLayout");

                            if (mainNode.node != null) {
                                // 主页
                                ANodeInfo listViewNode = mainNode.getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(1, "android.view.ViewGroup")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.view.ViewGroup")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.RelativeLayout");
                                // 第0和1都可能是ListView
                                ANodeInfo tmpNode = listViewNode.getChild(0, "android.widget.ListView");
                                if (tmpNode.node == null) {
                                    tmpNode = listViewNode.getChild(1, "android.widget.ListView");
                                }
                                listViewNode.recycle();
                                listViewNode = tmpNode;

                                if (listViewNode.node != null) {
                                    // 获取微信支付条目
                                    itemFor:for (int i = 0; i < listViewNode.getChildCount(); i++) {
                                        try {
                                            ANodeInfo msgItemNode = listViewNode.getChild(i, "android.widget.LinearLayout");
                                            // 有子节点并且子节点也是android.widget.LinearLayout则是消息条目
                                            if (msgItemNode.node != null) {
                                                if (msgItemNode.getChildCount() > 0) {
                                                    ANodeInfo msgItemRightNode = msgItemNode.getChildAndRecycle(0, "android.widget.LinearLayout")
                                                            .getChildAndRecycle(1, "android.widget.RelativeLayout")
                                                            .getChildAndRecycle(0, "android.widget.LinearLayout");

                                                    if (msgItemRightNode.node != null) {
                                                        String title = null, content = null, time = null;

                                                        ANodeInfo rightTopNode = msgItemRightNode.getChild(0, "android.widget.LinearLayout");
                                                        if (rightTopNode.node != null) {
                                                            ANodeInfo titleNode = rightTopNode.getChild(0).getChildAndRecycle(0);
                                                            if (titleNode.node != null) {
                                                                title = titleNode.getText();
                                                                titleNode.recycle();
                                                            }

                                                            ANodeInfo timeNode = rightTopNode.getChildAndRecycle(1);
                                                            if (timeNode.node != null) {
                                                                time = timeNode.getText();
                                                            }
                                                        }

                                                        ANodeInfo contentNode = msgItemRightNode.getChildAndRecycle(1, "android.widget.LinearLayout")
                                                                .getChildAndRecycle(0)
                                                                .getChildAndRecycle(0);
                                                        if (contentNode.node != null) {
                                                            content = contentNode.getText();
                                                            contentNode.recycle();
                                                        }

                                                        if (title != null && content != null && time != null && time.length() == 5 && time.equals(TimeUtils.getFormatTime("hour_minute").getString(System.currentTimeMillis()))) {
                                                            JSONObject result = getNodePayStr(title, content, paystrVal);
                                                            if (result != null) {
                                                                int amount = result.getIntValue("amount");
                                                                String type = result.getString("type");

                                                                synchronized (MyAccessibilityService.class) {
                                                                    if (!Application.getInstance().hasLastUp(packName, time, String.valueOf(amount))) {
                                                                        try {
                                                                            ApiExecUtil.pay(title, content, result.getBooleanValue("isUp"), purl, priKey, amount, type);
                                                                            Application.getInstance().setLastUp(packName, time, String.valueOf(amount));
                                                                        } catch (Exception e) {
                                                                            e.printStackTrace();
                                                                            Application.getInstance().sendNotify(NotifyPayService.NOTIFY_TITLE_DISPOSE_ERR, "处理出错：" + e.getMessage());
                                                                        }
                                                                    }
                                                                }

                                                                break itemFor;
                                                            }
                                                        }
                                                    }
                                                } else msgItemNode.recycle();
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            log.i("分析消息item结构出错，此条item忽略");
                                        }
                                    }
                                    listViewNode.recycle();
                                }
                            } else if (payMsgNode.node != null) {
                                // 支付消息页
                                ANodeInfo recycleViewNode = payMsgNode.getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.RelativeLayout")
                                        .getChildAndRecycle(0, "android.widget.LinearLayout")
                                        .getChildAndRecycle(1, "android.widget.FrameLayout")
                                        .getChildAndRecycle(1, "android.widget.LinearLayout")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(3, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(0, "android.widget.FrameLayout")
                                        .getChildAndRecycle(1, "androidx.recyclerview.widget.RecyclerView");

                                if (recycleViewNode.node != null) {
                                    // 拿到最后一个消息卡片
                                    ANodeInfo cardNode = recycleViewNode.getChildAndRecycle(recycleViewNode.getChildCount() - 1);

                                    // 第一个元素是时间
                                    ANodeInfo timeNode = cardNode.getChild(0, "android.widget.TextView");
                                    if (timeNode.node != null) {
                                        String title = null, content = null, time = timeNode.getText();
                                        timeNode.recycle();

                                        ANodeInfo cardTopNode = cardNode.getChildAndRecycle(1, "android.widget.FrameLayout")
                                                .getChildAndRecycle(0, "android.widget.LinearLayout")
                                                .getChildAndRecycle(0, "android.widget.LinearLayout")
                                                .getChildAndRecycle(0, "android.widget.FrameLayout")
                                                .getChildAndRecycle(0, "android.widget.LinearLayout")
                                                .getChildAndRecycle(0, "android.widget.RelativeLayout")
                                                .getChildAndRecycle(0, "android.widget.LinearLayout");

                                        if (cardTopNode.node != null) {
                                            ANodeInfo titleNode = cardTopNode.getChild(0, "android.widget.RelativeLayout")
                                                    .getChildAndRecycle(0, "android.widget.RelativeLayout")
                                                    .getChildAndRecycle(0, "android.widget.LinearLayout")
                                                    .getChildAndRecycle(0, "android.widget.LinearLayout")
                                                    .getChildAndRecycle(0, "android.widget.TextView");

                                            if (titleNode.node != null) {
                                                title = titleNode.getText();
                                                titleNode.recycle();
                                            }

                                            ANodeInfo contentNode = cardTopNode.getChildAndRecycle(1, "android.widget.LinearLayout");
                                            if (contentNode.node != null) {
                                                content = contentNode.getContentDescription();
                                                contentNode.recycle();
                                            }

                                            if (title != null && content != null && time != null && time.length() == 5 && time.equals(TimeUtils.getFormatTime("hour_minute").getString(System.currentTimeMillis()))) {
                                                JSONObject result = getNodePayStr(title, content, paystrVal);
                                                if (result != null) {
                                                    int amount = result.getIntValue("amount");
                                                    String type = result.getString("type");

                                                    synchronized (MyAccessibilityService.class) {
                                                        if (!Application.getInstance().hasLastUp(packName, time, String.valueOf(amount))) {
                                                            try {
                                                                ApiExecUtil.pay(title, content, result.getBooleanValue("isUp"), purl, priKey, amount, type);
                                                                Application.getInstance().setLastUp(packName, time, String.valueOf(amount));
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                                Application.getInstance().sendNotify(NotifyPayService.NOTIFY_TITLE_DISPOSE_ERR, "处理出错：" + e.getMessage());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    log.d("分析界面出错：" + e.getMessage());
                }
            }

        }

    }

    @Override
    public void onInterrupt() {
        log.d("accessibility interrupt");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        log.d("accessibility unbind");

        stopForeground(true);

        isRun = false;
        return super.onUnbind(intent);
    }

    /**
     * 从基础信息中获取包含指定包名的 paystr 下的对象的value
     * @param packName  包名
     * @param infoObj   基础信息
     * @return 包含指定包名的 paystr 下的对象的value，null包名未匹配，val多出了type代表paystr的key
     */
    private JSONObject getPaystrValByPackName(String packName, JSONObject infoObj) {
        JSONObject payObj = infoObj.getJSONObject("paystr");
        Set<String> poKeys = payObj.keySet();
        for (String poKey : poKeys) {
            // key是支付类型
            JSONObject poVal = payObj.getJSONObject(poKey);
            poVal.put("type", poKey);

            // 验证包名
            JSONArray poPackNames = poVal.getJSONArray("packName");
            for (Object popn : poPackNames) {
                if (packName.equals(popn)) {
                    return poVal;
                }
            }
        }
        return null;
    }

    /**
     * 获取基础信息对应的的匹配信息
     * @param title     标题
     * @param content   内容
     * @param poVal     基础信息中包含指定包名的值
     * @return JSON包含需要的信息amount,type,isUp，空代表未匹配
     */
    private JSONObject getNodePayStr(String title, String content, JSONObject poVal) {
        JSONObject result = new JSONObject();

        String poKey = poVal.getString("type");

        // 获取匹配列表
        JSONArray list = poVal.getJSONArray("list");
        if (list != null) {
            for (int j = 0; j < list.size(); j++) {
                JSONObject item = list.getJSONObject(j);

                // 校验是否可以匹配
                String checkTitle = item.getString("checkTitle");
                String checkContent = item.getString("checkContent");
                if ((checkTitle != null && !title.contains(checkTitle))
                        || (checkContent != null && !content.contains(checkContent))) {
                    log.i("item不匹配");
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

                    result.put("type", poKey);
                    result.put("amount", amount);
                    result.put("isUp", !poVal.containsKey("isUp") || poVal.getBooleanValue("isUp"));
                    return result;
                } catch (Exception e) {
                    log.i("非支付，金额非数字: " + amountStr);
                    continue;
                }
            }
        } else {
            Application.getInstance().sendNotify(NotifyPayService.NOTIFY_TITLE_DISPOSE_ERR, "配置的基础信息中，list为空, 类型=" + poKey);
        }

        return null;
    }

}
