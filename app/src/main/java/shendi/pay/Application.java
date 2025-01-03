package shendi.pay;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.TimeZone;

import shendi.kit.time.TimeUtils;
import shendi.pay.util.SQLiteUtil;

/**
 * 全局属性与初始化.
 * 创建时间：2023/11/8
 * @author 砷碲
 */
public class Application extends android.app.Application {

    /** 唯一实例 */
    private static Application instance;

    private static SLog log = SLog.getLogger(Application.class.getName());

    /** 数据库操作 */
    public static SQLiteUtil spaySql;

    /** 是否测试通知 */
    public boolean isTestNotify = false;

    public static final String NOTIFY_CHANNEL_ID_TEST = "Test";
    public static final String NOTIFY_CHANNEL_ID_PAY = "Pay";
    public static final String NOTIFY_CHANNEL_ID_ACCESSIBILITY = "Accessibility";

    // 广播字符串
    public static final String RECEIVE_TEST = "shendi.pay.receive.TestReceive";

    // SP BaseStore 部分
    /** 获取配置信息的URL地址 */
    private String basicInfoUrl;
    /** 用于验证的密钥 */
    private String basicPriKey;
    /** 用于无障碍开关 */
    private boolean basicAccessibility;
    /** 用于支付回调接口的自定义参数 */
    private String basicCustomParam;
    /** 从网络上获取的配置信息 */
    private JSONObject basicInfo;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 设置默认时区
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"));

        // 初始化基础信息
        SharedPreferences baseStore = getBaseStore();
        basicInfoUrl = baseStore.getString("infoUrl", null);
        String basicInfoStr = baseStore.getString("info", null);
        if (basicInfoStr != null) {
            try {
                basicInfo = JSON.parseObject(basicInfoStr);
            } catch (Exception e) {
                log.w("基础信息中 info 非JSONObject");
                sendNotify("基础信息 info 非JSONObject", basicInfoUrl);
            }
        }
        basicPriKey = baseStore.getString("priKey", null);
        try {
            basicAccessibility = baseStore.getBoolean("accessibility", true);
        } catch (Exception e) {
            log.w("基础信息中无障碍开关信息获取失败");
            sendNotify("基础信息初始化", "无障碍开关信息获取失败");
        }
        basicCustomParam = baseStore.getString("customParam", null);

        spaySql = SQLiteUtil.getInstance(this);

        // 时间格式
        TimeUtils.addTimeFormat("hour_minute", "HH:mm");
    }

    /** @return 唯一实例 */
    public static Application getInstance() {
        return instance;
    }

    public SharedPreferences getStore(String name) {
        return this.getSharedPreferences(name, MODE_PRIVATE);
    }

    /**
     {
        infoUrl : "获取配置信息的URL地址",
        priKey : "用于验证的密钥",
        customParam : "支付回调接口的自定义参数",
        // 从网络上获取的配置信息
        info : {},
        accessibility : "无障碍是否打开,boolean"
     }
     */
    public SharedPreferences getBaseStore() {
        return getStore("base");
    }

    /**
     * 无障碍使用的最后一次上交服务器信息,根据包名区分.
     {
        "包名" : {
            time : "时间",
            amount : "金额"
        }
     }
     */
    public SharedPreferences getLastUpStore() {
        return getStore("lastUp");
    }


    public static void showToast(Activity context, String text, int duration) {
        new Thread(() -> {
            context.runOnUiThread(() -> {
                Toast.makeText(context, text, duration).show();
            });
        }).start();
    }

    /**
     * 发送通知.
     * @param title     通知标题
     * @param content   通知内容
     */
    public void sendNotify(String title, String content) {
        // 获取NotifactionManager对象
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // 适配8.0及以上,创建渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(Application.NOTIFY_CHANNEL_ID_TEST, Application.NOTIFY_CHANNEL_ID_TEST, NotificationManager.IMPORTANCE_DEFAULT));
        }

        //构建一个Notification
        Notification n = new Notification.Builder(this, Application.NOTIFY_CHANNEL_ID_TEST)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(content)
                .setWhen(System.currentTimeMillis())
                .build();
        nm.notify((int) (System.currentTimeMillis() % 10000), n);

        log.i("发送了通知,title=[" + title + "],content=[" + content + "]");
    }


    // SP BaseStore 部分
    /**
     * 获取配置信息的URL地址
     * @param defVal 为空则默认值
     */
    public String getBasicInfoUrl(String defVal) {
        return basicInfoUrl == null ? defVal : basicInfoUrl;
    }
    /**
     * 设置配置信息的URL与URL对应的具体信息
     * @param infoUrl url地址
     * @param info    具体信息
     */
    public void setBasicInfoUrl(String infoUrl, JSONObject info) {
        getBaseStore().edit()
            .putString("infoUrl", infoUrl)
            .putString("info", info.toString())
            .apply();
        this.basicInfoUrl = infoUrl;
        this.basicInfo = info;
    }

    /** 用于验证的密钥 */
    public String getBasicPriKey(String defVal) {
        return basicPriKey == null ? defVal : basicPriKey;
    }
    /** 用于验证的密钥 */
    public void setBasicPriKey(String basicPriKey) {
        getBaseStore().edit()
            .putString("priKey", basicPriKey)
            .apply();
        this.basicPriKey = basicPriKey;
    }

    /** 用于无障碍开关 */
    public boolean getBasicAccessibility() {
        return basicAccessibility;
    }
    /** 用于无障碍开关 */
    public void setBasicAccessibility(boolean basicAccessibility) {
        getBaseStore().edit()
                .putBoolean("accessibility", basicAccessibility)
                .apply();
        this.basicAccessibility = basicAccessibility;
    }

    /** 用于支付回调接口的自定义参数 */
    public String getBasicCustomParam(String defVal) {
        return basicCustomParam == null ? defVal : basicCustomParam;
    }
    /** 用于验证的密钥 */
    public void setBasicCustomParam(String basicCustomParam) {
        getBaseStore().edit()
                .putString("customParam", basicCustomParam)
                .apply();
        this.basicCustomParam = basicCustomParam;
    }

    /** 从网络上获取的配置信息 */
    public JSONObject getBasicInfo() {
        return basicInfo;
    }

    // SP LastUpStore 部分

    /**
     * 指定包是否存在指定上传信息.
     * @param packName  包名称
     * @param time      时间字符串
     * @param amount    金额
     * @return 是否存在指定上传信息
     */
    public boolean hasLastUp(String packName, String time, String amount) {
        String val = getLastUpStore().getString(packName, null);

        if (val != null) {
            try {
                JSONObject obj = JSONObject.parseObject(val);
                String objTime = obj.getString("time");
                String objAmount = obj.getString("amount");

                if (time.equals(objTime) && amount.equals(objAmount)) {
                    return true;
                }
            } catch (Exception e) {}
        }

        return false;
    }

    /** 设置最后的上传信息 */
    public void setLastUp(String packName, String time, String amount) {
        JSONObject obj = new JSONObject(2);
        obj.put("time", time);
        obj.put("amount", amount);

        getLastUpStore().edit()
                .putString(packName, obj.toString())
                .apply();
    }

}
