package cn.yongye.androidability.common;

import android.util.Log;

import de.robv.android.xposed.DexposedBridge;

public class LogUtil {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String PRE_TAG = "AndroidAbility:";

    /**
     * Log.i .
     *
     * @param tag Log TAG .
     * @param msg Log msg .
     */
    public static void i(String tag, String msg) {
        if (DEBUG) {
            Log.i(PRE_TAG + tag, msg);
        }
    }

    /**
     * Log.d .
     *
     * @param tag Log TAG .
     * @param msg Log msg .
     */
    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(PRE_TAG + tag, msg);
        }
    }

    /**
     * Log.e .
     *
     * @param tag Log TAG .
     * @param msg Log msg .
     */
    public static void e(String tag, String msg) {
        Log.e(PRE_TAG + tag, msg);
    }

    /**
     * 打印调用堆栈.
     */
    public static void printStact(String tag) {
        //打印方法调用堆栈
        for (StackTraceElement stackTraceElement : new Throwable().getStackTrace()) {
            LogUtil.d(tag, "\t" + stackTraceElement.toString());
        }
    }
}
