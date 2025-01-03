package shendi.pay;

import static shendi.pay.Application.NOTIFY_CHANNEL_ID_TEST;

import android.accessibilityservice.AccessibilityService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import shendi.pay.service.MyAccessibilityService;

/**
 * 申请权限.
 * @author 砷碲
 */
public class Permission {

    /** 是否开通通知权限 */
    public static boolean isNotificationEnabled(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        return notificationManager.areNotificationsEnabled();
    }

    /** 检验是否开通通知权限，未开通则跳往开通 */
    public static void notify(Context context) {
        if (!isNotificationEnabled(context)) {
            Toast.makeText(context, "请开启通知权限", Toast.LENGTH_SHORT).show();

            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String packageName = Application.getInstance().getPackageName();
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFY_CHANNEL_ID_TEST);
            } else {
                intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", Application.getInstance().getPackageName());
                intent.putExtra("app_uid", Application.getInstance().getApplicationInfo().uid);
            }
            context.startActivity(intent);
        }
    }

    /**
     * 判断应用是否开启了通知监听权限
     */
    public static boolean isNotificationListenerEnabled(Context context) {
        String packageName = context.getPackageName();

        // 获取系统中所有启用了通知监听权限的包名
        String enabledListeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (enabledListeners != null && !TextUtils.isEmpty(enabledListeners)) {
            String[] packages = enabledListeners.split(":");
            for (String packageNameInList : packages) {
                if (packageNameInList.contains(packageName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void notifyListener(Context context) {
        if (!isNotificationListenerEnabled(context)) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            context.startActivity(intent);

            Toast.makeText(context, "请开启通知监听权限", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 检查无障碍服务是否已启用
     * @param context 上下文
     * @return true: 启用, false: 未启用
     */
    public static boolean isAccessibilityServiceEnabled(Context context) {
        return MyAccessibilityService.isRun;
    }

    /**
     * 无障碍服务是否启动，未启动跳转页面启动
     * @param context 上下文
     * @return true: 启用, false: 未启用
     */
    public static void accessibility(Context context) {
        if (!isAccessibilityServiceEnabled(context)) {
            try {
                context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception e) {
                context.startActivity(new Intent(Settings.ACTION_SETTINGS));
                e.printStackTrace();
            }

            Toast.makeText(context, "请开启本软件的无障碍服务 -" + context.getString(R.string.accessibility_service_label), Toast.LENGTH_LONG).show();
        }
    }



}
