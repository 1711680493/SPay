package shendi.pay;

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

import androidx.core.app.NotificationManagerCompat;

import shendi.pay.activity.TestActivity;
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

    // 广播字符串
    public static final String RECEIVE_TEST = "shendi.pay.receive.TestReceive";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        spaySql = SQLiteUtil.getInstance(this);
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
        // 从网络上获取的配置信息
        info : {}
     }
     */
    public SharedPreferences getBaseStore() {
        return getStore("base");
    }

    /** 是否开通通知权限 */
    public boolean isNotificationEnabled(Activity context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        return notificationManager.areNotificationsEnabled();
    }

    /** 检验是否开通通知权限，未开通则跳往开通 */
    public void checkNotify(Activity context) {
        if (!isNotificationEnabled(context)) {
            Toast.makeText(context, "请开启通知权限", Toast.LENGTH_SHORT).show();

            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String packageName = getPackageName();
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFY_CHANNEL_ID_TEST);
            } else {
                intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", getPackageName());
                intent.putExtra("app_uid", getApplicationInfo().uid);
            }
            context.startActivity(intent);
        }
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
        //获取NotifactionManager对象
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // 适配8.0及以上,创建渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(Application.NOTIFY_CHANNEL_ID_TEST, Application.NOTIFY_CHANNEL_ID_TEST, NotificationManager.IMPORTANCE_HIGH));
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

}
