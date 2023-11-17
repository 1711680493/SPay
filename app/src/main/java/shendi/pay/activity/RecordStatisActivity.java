package shendi.pay.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import shendi.kit.time.TimeUtils;
import shendi.pay.Application;
import shendi.pay.R;
import shendi.pay.SLog;
import shendi.pay.adapter.RecordStatisAdapter;
import shendi.pay.bean.NotifyPay;

/**
 * 记录与统计.
 * 创建时间：2023/11/8
 * @author Shendi
 */
public class RecordStatisActivity extends AppCompatActivity {

    private static SLog log = SLog.getLogger(RecordStatisActivity.class.getName());

    // 今日数据
    private TextView rsTodayAmountText,
                     rsTodayNumText,
                     rsTodaySuccessNumText;

    // 按钮与列表
    private Button rsModeBtn;
    private TextView rsPageText;
    private ListView rsList;
    private RecordStatisAdapter rsAdapter;

    /** 当前查询模式,0全部,1失败 */
    private int mode = 0;

    /** 当前页数 */
    private int pageNum = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_statis);

        getSupportActionBar().setTitle(R.string.bar_record_statis);
        // 启用ActionBar并显示返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initUI();
    }

    private void initUI() {
        SQLiteDatabase db = Application.spaySql.openReadLink();

        // 今日数据
        rsTodayAmountText = findViewById(R.id.rsTodayAmountText);
        rsTodayNumText = findViewById(R.id.rsTodayNumText);
        rsTodaySuccessNumText = findViewById(R.id.rsTodaySuccessNumText);

        String todayStr = TimeUtils.getFormatTime(TimeUtils.DATE).getString(System.currentTimeMillis());
        String todayFirst = String.valueOf(TimeUtils.getFormatTime().getNum(todayStr + " 00:00:00")),
               todayLast = String.valueOf(TimeUtils.getFormatTime().getNum(todayStr + " 23:59:59"));

        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM notify_pay WHERE time >= ? AND time <= ?", new String[]{todayFirst, todayLast});
        if (cursor.moveToFirst()) {
            double amount = cursor.getInt(0) / 100d;
            rsTodayAmountText.post(() -> { rsTodayAmountText.setText("￥" + amount); });
        }
        cursor.close();

        cursor = db.rawQuery("SELECT COUNT(*) FROM notify_pay WHERE time >= ? AND time <= ?", new String[]{todayFirst, todayLast});
        if (cursor.moveToFirst()) {
            int num = cursor.getInt(0);
            rsTodayNumText.post(() -> { rsTodayNumText.setText(String.valueOf(num)); });
        }
        cursor.close();

        cursor = db.rawQuery("SELECT COUNT(*) FROM notify_pay WHERE state=1 AND time >= ? AND time <= ?", new String[]{todayFirst, todayLast});
        if (cursor.moveToFirst()) {
            int num = cursor.getInt(0);
            rsTodaySuccessNumText.post(() -> { rsTodaySuccessNumText.setText(String.valueOf(num)); });
        }
        cursor.close();

        // 按钮与列表
        rsPageText = findViewById(R.id.rsPageText);
        rsList = findViewById(R.id.rsList);
        rsAdapter = new RecordStatisAdapter(this, R.layout.record_statis_item);
        rsList.setAdapter(rsAdapter);
        rsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NotifyPay np = rsAdapter.getItem(position);
                runOnUiThread(() -> {
                    new AlertDialog.Builder(RecordStatisActivity.this)
                            .setTitle("详细信息")
                            .setMessage(np.toString())
                            .create()
                            .show();
                });
            }
        });
        toPage(0);

        findViewById(R.id.rsModeBtn).setOnClickListener((v) -> {
            if (mode == 0) {
                mode = 1;
                ((Button) v).setText(R.string.record_statis_mode_fail);
            } else {
                mode = 0;
                ((Button) v).setText(R.string.record_statis_mode_all);
            }

            toPage(0);
        });

        findViewById(R.id.rsNextBtn).setOnClickListener((v) -> {
            toPage(pageNum+1);
        });
        findViewById(R.id.rsUpBtn).setOnClickListener((v) -> {
            if (pageNum != 0) toPage(pageNum-1);
        });

        findViewById(R.id.rsClearBtn).setOnClickListener((v) -> {
            runOnUiThread(() -> {
                new AlertDialog.Builder(RecordStatisActivity.this)
                        .setTitle("确认清除")
                        .setMessage("是否确认清除所有记录？")
                        .setPositiveButton("清除", (dialog, which) -> {
                            Application.spaySql.openWriteLink().execSQL("DELETE FROM notify_pay");
                            Application.showToast(this, "清除成功", Toast.LENGTH_SHORT);
                            initUI();
                        })
                        .setNeutralButton("取消", (dialog, which) -> {})
                        .create()
                        .show();
            });
        });
    }

    /** 查询指定页的数据,0为第一页 */
    @SuppressLint("Range")
    private void toPage(int pageNum) {
        this.pageNum = pageNum;
        rsAdapter.clear();

        String sql;
        if (mode == 0) {
            sql = "SELECT * FROM notify_pay ORDER BY id DESC LIMIT 50 OFFSET " + (pageNum * 50);
        } else {
            sql = "SELECT * FROM notify_pay WHERE state=0 ORDER BY id DESC LIMIT 50 OFFSET " + (pageNum * 50);
        }

        Cursor cursor = Application.spaySql.openReadLink().rawQuery(sql, null);
        while (cursor.moveToNext()) {
            NotifyPay np = new NotifyPay();
            np.setId(cursor.getLong(cursor.getColumnIndex("id")));
            np.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            np.setContent(cursor.getString(cursor.getColumnIndex("content")));
            np.setType(cursor.getString(cursor.getColumnIndex("type")));
            np.setAmount(cursor.getLong(cursor.getColumnIndex("amount")));
            np.setState(cursor.getInt(cursor.getColumnIndex("state")));
            np.setReason(cursor.getString(cursor.getColumnIndex("reason")));
            np.setTime(cursor.getLong(cursor.getColumnIndex("time")));

            rsAdapter.add(np);
        }
        cursor.close();

        rsPageText.post(() -> {
            rsPageText.setText("第" + (pageNum+1) + "页");
        });
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
        log.i("record statis destroy");
    }
}
