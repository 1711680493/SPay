package shendi.pay.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import shendi.pay.Application;
import shendi.pay.Permission;
import shendi.pay.R;
import shendi.pay.SLog;
import shendi.pay.service.MyAccessibilityService;
import shendi.pay.service.NotifyPayService;

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
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 校验权限是否开通
        Permission.notify(this);
        Permission.notifyListener(this);

        if (Application.getInstance().getBasicAccessibility()) {
            Permission.accessibility(this);
        }
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
