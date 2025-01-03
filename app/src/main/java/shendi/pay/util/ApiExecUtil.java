package shendi.pay.util;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import shendi.pay.Application;

/**
 * API 具体执行接口封装.
 * 创建时间：2025/1/2
 * @author 砷碲
 */
public class ApiExecUtil {

    /**
     * 调用支付回调接口的封装，包含数据库操作.
     * @param title     标题
     * @param content   内容
     * @param isUp      是否上传
     * @param url       回调接口地址
     * @param priKey    私钥
     * @param amount    金额,分
     * @param type      类型
     * @throws Exception 出错时抛出
     */
    public static void pay(String title, String content, boolean isUp, String url, String priKey, int amount, String type) throws Exception {
        long time = System.currentTimeMillis();

        // 加入数据库
        SQLiteDatabase db = Application.spaySql.openWriteLink();

        ContentValues sqlValues = new ContentValues();
        sqlValues.put("title", title);
        sqlValues.put("content", content);
        sqlValues.put("type", type);
        sqlValues.put("amount", amount);
        sqlValues.put("time", time);

        long insertId = db.insert("notify_pay", null, sqlValues);

        // 调用支付回调接口
        if (isUp) {
            ApiUtil.pay(url, priKey, amount, type, time, (res) -> {
                String code = res.getString("code");
                if ("10000".equals(code)) {
                    db.execSQL("UPDATE notify_pay SET state=1 WHERE id=?", new Object[]{insertId});
                } else {
                    StringBuilder reason = new StringBuilder();
                    reason.append(code).append(" - ").append(res.getString("msg"));
                    db.execSQL("UPDATE notify_pay SET reason=? WHERE id=?", new Object[]{reason, insertId});
                }
            }, (err) -> {
                db.execSQL("UPDATE notify_pay SET reason=? WHERE id=?", new Object[]{err.getString("errMsg"), insertId});
            });
        } else {
            db.execSQL("UPDATE notify_pay SET state=1 WHERE id=?", new Object[]{insertId});
        }
    }

}
