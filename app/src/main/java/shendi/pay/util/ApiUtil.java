package shendi.pay.util;

import com.alibaba.fastjson2.JSONObject;

import java.net.URLEncoder;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import shendi.pay.Application;
import shendi.pay.SLog;

/**
 * 所有接口调用的封装.
 *
 * 创建时间：2023/11/7
 * @author Shendi
 */
public class ApiUtil {

    private static SLog log = SLog.getLogger(ApiUtil.class.getName());

    public static OkHttpClient okhttp = new OkHttpClient();

    /** 回调接口 */
    public interface Callback {

        /**
         * 回调函数.
         * @param data api调用执行的结果
         */
        void callback(JSONObject data);

    }

    /**
     * 获取使用需要的基本配置.
     * @param url       获取配置的地址
     * @param success   成功的回调
     * @param fail      失败的回调
     */
    public static void baseConfig(String url, Callback success, Callback fail) {
        JSONObject obj = new JSONObject();
        obj.put("url", url);
        obj.put("success", success);
        obj.put("fail", fail);

        call(obj);
    }

    /**
     * 调用支付回调接口.
     * @param url       支付回调地址
     * @param priKey    密钥
     * @param amount    支付金额,单位分
     * @param type      支付类型
     * @param success   成功的回调
     * @param fail      失败的回调
     */
    public static void pay(String url, String priKey, int amount, String type, Callback success, Callback fail) {
        pay(url, priKey, amount, type, System.currentTimeMillis(), success, fail);
    }

    /**
     * 调用支付回调接口.
     * @param url       支付回调地址
     * @param priKey    密钥
     * @param amount    支付金额,单位分
     * @param type      支付类型
     * @param time      时间戳
     * @param success   成功的回调
     * @param fail      失败的回调
     */
    public static void pay(String url, String priKey, int amount, String type, long time, Callback success, Callback fail) {
        JSONObject obj = new JSONObject();
        obj.put("url", url);
        obj.put("type", "POST");
        JSONObject param = new JSONObject();
        param.put("amount", amount);
        param.put("type", type);
        param.put("priKey", priKey);
        param.put("time", time);
        obj.put("param", param);
        obj.put("success", success);
        obj.put("fail", fail);

        call(obj);
    }

    /**
     * 调用api，参考js的ajax，其中obj的某些参数不想被修改则增加一个新参数为 is当前参数名(驼峰式),例如不想修改url，那么 isUrl = true
     * 其中失败的回调，errMsg代表错误信息
     * @param obj       参数
     */
    public static void call(JSONObject obj) {
        new Thread(() -> {
            try {
                String url = obj.getString("url");
                String type = obj.getString("type");
                JSONObject param = obj.getJSONObject("param");
                JSONObject heads = obj.getJSONObject("heads");
                Integer timeout = obj.getInteger("timeout");

                if (type == null) type = "GET";
                if (heads == null) heads = new JSONObject();
                if (timeout == null) timeout = 5000;

                StringBuilder paramStr = new StringBuilder();
                if (param != null) {
                    for (Map.Entry<String, Object> kv : param.entrySet()) {
                        if (kv.getValue() == null) continue;
                        paramStr.append("&").append(kv.getKey()).append("=").append(URLEncoder.encode(kv.getValue().toString(), "UTF-8"));
                    }
                    if (paramStr.length() != 0) paramStr.deleteCharAt(0);
                }

                boolean isPost = "POST".equalsIgnoreCase(type);
                if (!isPost) {
                    if (param != null) {
                        paramStr.insert(0, '?');
                        // 非post将数据放在url
                        url += paramStr.toString();
                    }
                } else {
                    heads.put("Content-Type", "application/x-www-form-urlencoded");
                    heads.put("Content-Length", paramStr.toString());
                }

                log.i(type + " " + url + " [" + paramStr + "]");

                // 创建请求
                Request.Builder reqBuild = new Request.Builder().url(url);

                // 设置请求头
                for (Map.Entry<String, Object> kv : heads.entrySet()) {
                    reqBuild.header(kv.getKey(), kv.getValue().toString());
                }

                if (isPost) {
                    reqBuild.post(RequestBody.create(paramStr.toString().getBytes()));
                } else {
                    switch (type) {
                        case "GET": reqBuild.get(); break;
                        case "head" : reqBuild.head(); break;
                    }
                }

                Call call = okhttp.newCall(reqBuild.build());
                Response response = call.execute();
                ResponseBody body = response.body();
                byte[] data = body.bytes();
                log.i(url + " OK, Data Len=" + data.length);

                // 回调
                Callback success = obj.getObject("success", Callback.class);
                JSONObject result = null;
                if (!obj.containsKey("isSuccess")) {
                    result = JSONObject.parseObject(new String(data));
                } else {
                    result = new JSONObject();
                    result.put("data", data);
                }

                if (success != null) success.callback(result);
            } catch (Exception e) {
                e.printStackTrace();
                log.e("api call error: " + e.getMessage());

                // 有error则调用error
                Callback fail = obj.getObject("fail", Callback.class);

                if (fail != null) {
                    JSONObject error = new JSONObject();
                    error.put("err", e);
                    error.put("errMsg", e.getMessage());

                    fail.callback(error);
                }
            }
        }).start();
    }

}
