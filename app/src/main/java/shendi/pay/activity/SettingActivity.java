package shendi.pay.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import shendi.pay.Application;
import shendi.pay.Permission;
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

    // 自定义参数
    private EditText settingCustomParamEdit;

    // 当前基础信息
    private TextView settingCurText;

    // 无障碍开关
    private Switch settingAccessibilitySwitch;

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
        settingBaseUrlEdit.setText(Application.getInstance().getBasicInfoUrl(""));
        findViewById(R.id.settingBaseBtn).setOnClickListener((v) -> {
            String url = settingBaseUrlEdit.getText().toString();
            ApiUtil.baseConfig(url, (res) -> {
                String code = res.getString("code");
                if ("10000".equals(code)) {
                    try {
                        JSONObject info = JSON.parseObject(res.getString("msg"));
                        Application.getInstance().setBasicInfoUrl(url, info);

                        settingCurText.post(() -> {
                            settingCurText.setText(info.toString());
                        });
                    } catch (Exception e) {
                        Application.showToast(this, "基础信息非JSON，转换出错：" + e.getMessage(), Toast.LENGTH_LONG);
                    }
                } else {
                    Application.showToast(this, "接口调用失败，错误码：" + code, Toast.LENGTH_SHORT);
                }
            }, (err) -> {
                Application.showToast(this, err.getString("errMsg"), Toast.LENGTH_LONG);
            });
        });

        // 密钥
        settingKeyEdit = findViewById(R.id.settingKeyEdit);
        settingKeyEdit.setText(Application.getInstance().getBasicPriKey(""));
        findViewById(R.id.settingKeyBtn).setOnClickListener((v) -> {
            Application.getInstance().setBasicPriKey(settingKeyEdit.getText().toString());
            Application.showToast(SettingActivity.this, "设置成功", Toast.LENGTH_SHORT);
        });

        // 自定义参数
        settingCustomParamEdit = findViewById(R.id.settingCustomParamEdit);
        settingCustomParamEdit.setText(Application.getInstance().getBasicCustomParam(""));
        findViewById(R.id.settingCustomParamBtn).setOnClickListener((v) -> {
            Application.getInstance().setBasicCustomParam(settingCustomParamEdit.getText().toString());
            Application.showToast(SettingActivity.this, "设置成功", Toast.LENGTH_SHORT);
        });

        // 无障碍开关
        settingAccessibilitySwitch = findViewById(R.id.settingAccessibilitySwitch);
        settingAccessibilitySwitch.setChecked(Application.getInstance().getBasicAccessibility());
        settingAccessibilitySwitch.setOnCheckedChangeListener((v, isChecked) -> {
            Application.getInstance().setBasicAccessibility(isChecked);
            if (isChecked) {
                Permission.accessibility(SettingActivity.this);
            }
        });

        // 当前基础信息
        settingCurText = findViewById(R.id.settingCurText);
        JSONObject basicInfo = Application.getInstance().getBasicInfo();
        settingCurText.setText(basicInfo == null ? "" : basicInfo.toString());
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
