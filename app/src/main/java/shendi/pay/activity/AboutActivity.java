package shendi.pay.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import shendi.pay.R;
import shendi.pay.SLog;

/**
 * 关于.
 * 创建时间：2023/11/8
 * @author Shendi
 */
public class AboutActivity extends AppCompatActivity {

    private static SLog log = SLog.getLogger(AboutActivity.class.getName());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        getSupportActionBar().setTitle(R.string.bar_about);
        // 启用ActionBar并显示返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        log.i("about destroy");
    }
}
