package shendi.pay.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import shendi.pay.Application;
import shendi.pay.R;
import shendi.pay.SLog;
import shendi.pay.service.NotifyPayService;
import shendi.pay.util.ApiUtil;

/**
 * 创建时间：2023/11/8
 * @author Shendi
 */
public class MainActivity extends Activity {

    private static SLog log = SLog.getLogger(MainActivity.class.getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initUI();

        //启动服务
        startForegroundService(new Intent(this, NotifyPayService.class));

        // 检验是否开通通知权限
        Application.getInstance().checkNotify(this);
    }

    private void initUI() {
        findViewById(R.id.recordStatisBtn).setOnClickListener((v) -> {
            startActivity(new Intent(this, RecordStatisActivity.class));
        });
        findViewById(R.id.settingBtn).setOnClickListener((v) -> {
            startActivity(new Intent(this, SettingActivity.class));
        });
        findViewById(R.id.testBtn).setOnClickListener((v) -> {
            startActivity(new Intent(this, TestActivity.class));
        });
        findViewById(R.id.aboutBtn).setOnClickListener((v) -> {
            startActivity(new Intent(this, AboutActivity.class));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log.i("main destroy");
    }
}
