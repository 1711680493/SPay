package shendi.pay.activity;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import shendi.pay.Application;
import shendi.pay.R;
import shendi.pay.SLog;
import shendi.pay.util.ApiUtil;

/**
 * 设置.
 * 创建时间：2023/11/8
 * @author Shendi
 */
public class SettingActivity extends AppCompatActivity {

    private static SLog log = SLog.getLogger(SettingActivity.class.getName());

    // 基础信息
    private EditText settingBaseUrlEdit;

    // 密钥
    private EditText settingKeyEdit;

    // 当前基础信息
    private TextView settingCurText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        getSupportActionBar().setTitle(R.string.bar_setting);
        // 启用ActionBar并显示返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initUI();
    }

    private void initUI() {
        // 基础信息
        settingBaseUrlEdit = findViewById(R.id.settingBaseUrlEdit);
        settingBaseUrlEdit.setText(Application.getInstance().getBaseStore().getString("infoUrl", ""));
        findViewById(R.id.settingBaseBtn).setOnClickListener((v) -> {
            ApiUtil.baseConfig(settingBaseUrlEdit.getText().toString(), (res) -> {
                String code = res.getString("code");
                if ("10000".equals(code)) {
                    Application.getInstance().getBaseStore().edit()
                            .putString("infoUrl", settingBaseUrlEdit.getText().toString())
                            .putString("info", res.getString("msg"))
                            .apply();

                    settingCurText.post(() -> {
                        settingCurText.setText(Application.getInstance().getBaseStore().getString("info", ""));
                    });
                } else {
                    Application.showToast(this, "接口调用失败，错误码：" + code, Toast.LENGTH_SHORT);
                }
            }, (err) -> {
                Application.showToast(this, err.getString("errMsg"), Toast.LENGTH_LONG);
            });
        });

        // 密钥
        settingKeyEdit = findViewById(R.id.settingKeyEdit);
        settingKeyEdit.setText(Application.getInstance().getBaseStore().getString("priKey", ""));
        findViewById(R.id.settingKeyBtn).setOnClickListener((v) -> {
            Application.getInstance().getBaseStore().edit()
                    .putString("priKey", settingKeyEdit.getText().toString())
                    .apply();
        });

        // 当前基础信息
        settingCurText = findViewById(R.id.settingCurText);
        settingCurText.setText(Application.getInstance().getBaseStore().getString("info", ""));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // 返回箭头完成当前Activity
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log.i("setting destroy");
    }
}
