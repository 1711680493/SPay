package shendi.pay;

import android.util.Log;

/**
 * 封装日志输出操作，以便后续扩展.
 * 创建时间：2023/11/8
 * @author Shendi
 */
public class SLog {

    private String mTag;

    private SLog(String tag){
        mTag = tag;
    }

    public static SLog getLogger(String tag) {
        return new SLog("spay_"+tag);
    }

    public void v(String msg){
        Log.v(mTag,msg);
    }

    public void d(String msg){
        Log.d(mTag,msg);
    }

    public void i(String msg){
        Log.i(mTag,msg);
    }

    public void w(String msg){
        Log.w(mTag,msg);
    }

    public void e(String msg){
        Log.e(mTag,msg);
    }

}
