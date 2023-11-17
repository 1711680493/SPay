package shendi.pay.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.Date;

import shendi.pay.Application;
import shendi.pay.R;
import shendi.pay.SLog;
import shendi.pay.util.ApiUtil;

/**
 * 测试.
 * 创建时间：2023/11/8
 * @author Shendi
 */
public class TestActivity extends AppCompatActivity {

    private static SLog log = SLog.getLogger(TestActivity.class.getName());

    /** 广播接收器 */
    private TestReceive receive;

    // 基础信息接口
    private EditText testIBaseUrlEdit;

    // 支付回调接口
    private EditText testIPayUrlEdit,
                     testIPayPriKeyEdit,
                     testIPayAmountEdit,
                     testIPayTypeEdit;

    // 通知测试
    private EditText testNotifyTitleEdit,
            testNotifyContentEdit;
    private ListView testNotifyList;
    private ArrayAdapter<String> testNotifyListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);

        getSupportActionBar().setTitle(R.string.bar_test);
        // 启用ActionBar并显示返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initUI();

        //注册广播
        receive = new TestReceive();
        registerReceiver(receive, new IntentFilter(Application.RECEIVE_TEST));
    }

    private void initUI() {
        // 基础信息接口
        testIBaseUrlEdit = findViewById(R.id.testIBaseUrlEdit);
        findViewById(R.id.testIBaseBtn).setOnClickListener((v) -> {
            ApiUtil.baseConfig(testIBaseUrlEdit.getText().toString(), (res) -> {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle("请求结果")
                            .setMessage(res.toString())
                            .create()
                            .show();
                });
            }, (err) -> {
                Application.showToast(this, err.getString("errMsg"), Toast.LENGTH_LONG);
            });
        });

        // 支付回调接口
        testIPayUrlEdit = findViewById(R.id.testIPayUrlEdit);
        testIPayPriKeyEdit = findViewById(R.id.testIPayPriKeyEdit);
        testIPayAmountEdit = findViewById(R.id.testIPayAmountEdit);
        testIPayTypeEdit = findViewById(R.id.testIPayTypeEdit);
        findViewById(R.id.testIPayBtn).setOnClickListener((v) -> {
            String payUrl = testIPayUrlEdit.getText().toString(),
                   payPriKey = testIPayPriKeyEdit.getText().toString(),
                   payType = testIPayTypeEdit.getText().toString();

            int payAmount = 0;
            try {
                payAmount = (int) (Double.parseDouble(testIPayAmountEdit.getText().toString()) * 100);
            } catch (Exception e) {
                Application.showToast(this, "请输入正确的金额，单位元", Toast.LENGTH_SHORT);
                return;
            }

            ApiUtil.pay(payUrl, payPriKey, payAmount, payType, (res) -> {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle("请求结果")
                            .setMessage(res.toString())
                            .create()
                            .show();
                });
            }, (err) -> {
                Application.showToast(this, err.getString("errMsg"), Toast.LENGTH_LONG);
            });
        });
        findViewById(R.id.testIPayUseConfigBtn).setOnClickListener((v) -> {
            String payType = testIPayTypeEdit.getText().toString();

            int payAmount = 0;
            try {
                payAmount = (int) (Double.parseDouble(testIPayAmountEdit.getText().toString()) * 100);
            } catch (Exception e) {
                Application.showToast(this, "请输入正确的金额，单位元", Toast.LENGTH_SHORT);
                return;
            }

            String infoStr = Application.getInstance().getBaseStore().getString("info", null);
            String priKey = Application.getInstance().getBaseStore().getString("priKey", null);
            if (infoStr == null || priKey == null) {
                Application.showToast(this, "没有配置基础信息或密钥,无法请求", Toast.LENGTH_LONG);
                return;
            }

            try {
                ApiUtil.pay(JSON.parseObject(infoStr).getString("purl"), priKey, payAmount, payType, (res) -> {
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(this)
                                .setTitle("请求结果")
                                .setMessage(res.toString())
                                .create()
                                .show();
                    });
                }, (err) -> {
                    Application.showToast(this, err.getString("errMsg"), Toast.LENGTH_LONG);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Application.showToast(this, "请求出错：" + e.getMessage(), Toast.LENGTH_LONG);
            }
        });

        // 通知测试
        testNotifyTitleEdit = findViewById(R.id.testNotifyTitleEdit);
        testNotifyContentEdit = findViewById(R.id.testNotifyContentEdit);
        testNotifyList = findViewById(R.id.testNotifyList);
        testNotifyListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        testNotifyList.setAdapter(testNotifyListAdapter);

        // 发送通知
        findViewById(R.id.testNotifySendBtn).setOnClickListener((v) -> {
            // 检验是否开通通知权限
            Application.getInstance().checkNotify(this);

            Application.getInstance().sendNotify(testNotifyTitleEdit.getText().toString(), testNotifyContentEdit.getText().toString());
        });
        // 测试通知
        findViewById(R.id.testNotifyBtn).setOnClickListener((v) -> {
            Application.getInstance().isTestNotify = !Application.getInstance().isTestNotify;
            // 设置文本
            ((Button) v).setText(Application.getInstance().isTestNotify ? R.string.test_notify_close : R.string.test_notify_open);
        });
    }

    /**
     * 广播接收
     */
    public class TestReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            log.i("Test接收到广播");
            String info = intent.getStringExtra("info");
            testNotifyListAdapter.insert(info, 0);
        }
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

        // 停止测试通知
        Application.getInstance().isTestNotify = false;

        // 注销广播接收器
        unregisterReceiver(receive);

        log.i("test destroy");
    }
}
