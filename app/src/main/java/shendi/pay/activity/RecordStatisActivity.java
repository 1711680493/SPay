package shendi.pay.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
    private Button rsFilterBtn;
    private TextView rsPageText;
    private ListView rsList;
    private RecordStatisAdapter rsAdapter;

    // 底部的文本框
    private TextView rsBItem;
    private TextView rsBPage;

    /** 过滤的对话框 */
    private AlertDialog filterDialog;
    /** 值-状态 */
    private Integer dialogState;
    private int dialogSYear;
    private int dialogSMonth;
    private int dialogSDay;
    private Long dialogSTime;

    private int dialogEYear;
    private int dialogEMonth;
    private int dialogEDay;
    private Long dialogETime;

    /** 当前页数 */
    private int pageNum = 0;
    /** 一页数量 */
    private int pageCount = 50;

    /** 筛选的条件 */
    private NotifyPay dataFilter = new NotifyPay();
    /** 筛选的字符串,缓存 */
    private StringBuilder filterSql = new StringBuilder();
    /** 筛选的对象,缓存 */
    private String[] filterArgs = new String[0];

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

        // -今日数据
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

        // -构建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogInflater = this.getLayoutInflater().inflate(R.layout.dialog_record_statis, null);
        builder.setView(dialogInflater)
                .setPositiveButton(R.string.filter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String drsType = ((TextView) dialogInflater.findViewById(R.id.drsType)).getText().toString();
                        dataFilter.setType("".equals(drsType) ? null : drsType);
                        dataFilter.setState(dialogState);
                        dataFilter.sTime = dialogSTime;
                        dataFilter.eTime = dialogETime;

                        filter();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        filterDialog.cancel();
                    }
                });
        filterDialog = builder.create();
        ((Spinner) dialogInflater.findViewById(R.id.drsState)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                if (pos == 1) dialogState = 1;
                else if (pos == 2) dialogState = 0;
                else dialogState = null;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                log.d("没有选择");
            }
        });

        Calendar calendar = Calendar.getInstance();
        dialogSYear = dialogEYear = calendar.get(Calendar.YEAR);
        dialogSMonth = dialogEMonth = calendar.get(Calendar.MONTH) + 1;
        dialogSDay = dialogEDay = calendar.get(Calendar.DAY_OF_MONTH);
        dialogSTime = dialogETime = null;
        TextView drsSTime = dialogInflater.findViewById(R.id.drsSTime);
        drsSTime.setOnClickListener((v) -> {
            new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    dialogSYear = year;
                    dialogSMonth = month + 1;
                    dialogSDay = day;

                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    dialogSTime = calendar.getTimeInMillis();

                    drsSTime.post(() -> {
                        drsSTime.setText(dialogSYear + "-" + dialogSMonth + "-" + dialogSDay);
                    });
                }
            }, dialogSYear, dialogSMonth - 1, dialogSDay).show();
        });
        TextView drsETime = dialogInflater.findViewById(R.id.drsETime);
        drsETime.setOnClickListener((v) -> {
            new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    dialogEYear = year;
                    dialogEMonth = month + 1;
                    dialogEDay = day;

                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    dialogETime = calendar.getTimeInMillis();

                    drsETime.post(() -> {
                        drsETime.setText(dialogEYear + "-" + dialogEMonth + "-" + dialogEDay);
                    });
                }
            }, dialogEYear, dialogEMonth - 1, dialogEDay).show();
        });

        // -按钮与列表
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

        findViewById(R.id.rsFilterBtn).setOnClickListener((v) -> {
            // 显示弹窗
            filterDialog.show();
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

        // -底部信息
        rsBItem = findViewById(R.id.rsBItem);
        rsBPage = findViewById(R.id.rsBPage);
        findViewById(R.id.rsBStatisBtn).setOnClickListener((v) -> {
            StringBuilder sql = new StringBuilder();
            List<String> args = new ArrayList<>();

            if (dataFilter.getType() != null) { sql.append(" AND type=?"); args.add(dataFilter.getType()); }
            if (dataFilter.sTime != null) { sql.append(" AND time>=?"); args.add(String.valueOf(dataFilter.sTime)); }
            if (dataFilter.eTime != null) { sql.append(" AND time<=?"); args.add(String.valueOf(dataFilter.eTime)); }
            if (sql.length() != 0) sql.delete(0, 4).insert(0, " WHERE");

            SQLiteDatabase sdb = Application.spaySql.openReadLink();

            // 交易笔数，交易金额，成功笔数，成功金额，失败笔数，失败金额
            StringBuilder ssBuild = new StringBuilder("统计：\n\n");

            String[] argStrs = args.toArray(new String[0]);
            Cursor c = sdb.rawQuery("SELECT COUNT(*) FROM notify_pay" + sql, argStrs);
            if (c.moveToFirst()) {
                ssBuild.append("交易笔数：").append(c.getInt(0));
            }
            c.close();
            c = sdb.rawQuery("SELECT SUM(amount) FROM notify_pay" + sql, argStrs);
            if (c.moveToFirst()) {
                ssBuild.append("，交易金额：").append(c.getInt(0) / 100d);
            }
            c.close();

            String tmp = sql.indexOf("WHERE") == -1 ? " WHERE state=1" : sql + " AND state=1";
            c = db.rawQuery("SELECT COUNT(*) FROM notify_pay" + tmp, argStrs);
            if (c.moveToFirst()) {
                ssBuild.append("\n成功笔数：").append(c.getInt(0));
            }
            c.close();
            c = db.rawQuery("SELECT SUM(amount) FROM notify_pay" + tmp, argStrs);
            if (c.moveToFirst()) {
                ssBuild.append("，成功金额：").append(c.getInt(0) / 100d);
            }
            c.close();

            tmp = sql.indexOf("WHERE") == -1 ? " WHERE state=0" : sql + " AND state=0";
            c = db.rawQuery("SELECT COUNT(*) FROM notify_pay" + tmp, argStrs);
            if (c.moveToFirst()) {
                ssBuild.append("\n失败笔数：").append(c.getInt(0));
            }
            c.close();
            c = db.rawQuery("SELECT SUM(amount) FROM notify_pay" + tmp, argStrs);
            if (c.moveToFirst()) {
                ssBuild.append("，失败金额：").append(c.getInt(0) / 100d);
            }
            c.close();

            new AlertDialog.Builder(this)
                    .setMessage(ssBuild.toString())
                    .setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .create().show();
        });

        // 初始化过滤数据
        filter();
    }

    /** 过滤操作,过滤按钮点击 */
    private void filter() {
        filterSql = filterSql();
        List<String> flist = filterList();
        filterArgs = flist.toArray(new String[0]);

        // 获取页数信息
        SQLiteDatabase db = Application.spaySql.openReadLink();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM notify_pay" + filterSql, filterArgs);
        if (cursor.moveToFirst()) {
            int num = cursor.getInt(0);
            rsBItem.post(() -> {
                rsBItem.setText(String.valueOf(num));
                rsBPage.setText(String.valueOf(num / pageCount + 1));
            });
        }
        cursor.close();

        toPage(0);
    }

    /** 查询指定页的数据,0为第一页 */
    @SuppressLint("Range")
    private void toPage(int pageNum) {
        this.pageNum = pageNum;
        rsAdapter.clear();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM notify_pay").append(filterSql)
                .append(" ORDER BY id DESC LIMIT ").append(pageCount).append(" OFFSET ").append(pageNum * pageCount);

        Cursor cursor = Application.spaySql.openReadLink().rawQuery(sql.toString(), filterArgs);
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

    /**
     * 组装用于过滤的sql,携带WHERE.
     * @return 组装后的sql字符串
     */
    private StringBuilder filterSql() {
        StringBuilder sql = new StringBuilder();

        // 状态，类型，日期区间
        if (dataFilter.getState() != null) sql.append(" AND state=?");
        if (dataFilter.getType() != null) sql.append(" AND type=?");
        if (dataFilter.sTime != null) sql.append(" AND time>=?");
        if (dataFilter.eTime != null) sql.append(" AND time<=?");

        if (sql.length() != 0) sql.delete(0, 4).insert(0, " WHERE");
        return sql;
    }

    /**
     * 组装用于过滤的内容列表
     * @return 列表
     */
    private List<String> filterList() {
        List<String> list = new ArrayList<>();

        // 状态，类型，日期区间
        if (dataFilter.getState() != null) list.add(String.valueOf(dataFilter.getState()));
        if (dataFilter.getType() != null) list.add(dataFilter.getType());
        if (dataFilter.sTime != null) list.add(String.valueOf(dataFilter.sTime));
        if (dataFilter.eTime != null) list.add(String.valueOf(dataFilter.eTime));

        return list;
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
