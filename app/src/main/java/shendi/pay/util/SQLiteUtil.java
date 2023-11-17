package shendi.pay.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库工具封装。
 * 创建时间：2023/11/8
 * @author Shendi
 */
public class SQLiteUtil extends SQLiteOpenHelper {

    private static final String DB_NAME = "spay.db";
    private static final int DB_VERSION = 1;
    private static SQLiteUtil mHelper = null;
    private static SQLiteDatabase mReadDatabase = null;
    private static SQLiteDatabase mWriteDatabase = null;

    //单例模式
    public static SQLiteUtil getInstance(Context context) {
        if (mHelper == null) {
            synchronized (SQLiteUtil.class) {
                if (mHelper == null) mHelper = new SQLiteUtil(context);
            }
        }
        return mHelper;
    }
    public static SQLiteUtil getInstance(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        if (mHelper == null) {
            synchronized (SQLiteUtil.class) {
                if (mHelper == null) mHelper = new SQLiteUtil(context, name, factory, version);
            }
        }
        return mHelper;
    }

    private SQLiteUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    private SQLiteUtil(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public SQLiteDatabase openWriteLink() {
        if (mWriteDatabase == null || !mWriteDatabase.isOpen()) {
            mWriteDatabase = mHelper.getWritableDatabase();
        }
        return mWriteDatabase;
    }

    public SQLiteDatabase openReadLink() {
        if (mReadDatabase == null || !mReadDatabase.isOpen()) {
            mReadDatabase = mHelper.getReadableDatabase();
        }
        return mReadDatabase;
    }

    public void closeLink() {
        if (mReadDatabase != null && mReadDatabase.isOpen()) {
            mReadDatabase.close();
            mReadDatabase = null;
        }
        if (mWriteDatabase != null && mWriteDatabase.isOpen()) {
            mWriteDatabase.close();
            mWriteDatabase = null;
        }
    }

    //数据库初始化时需要进行的一些操作，比如创建数据表
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS notify_pay(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "title VARCHAR(100) NOT NULL," +
                "content VARCHAR(255)," +
                // 类型
                "type VARCHAR(255)," +
                // 金额，单位分
                "amount INTEGER," +
                // 状态，0失败,1成功
                "state TINYINT DEFAULT 0," +
                // 失败的原因
                "reason VARCHAR(100) DEFAULT '请求未成功'," +
                // 时间戳
                "time INTEGER);";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

}
