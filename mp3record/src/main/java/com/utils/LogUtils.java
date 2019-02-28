package com.utils;

import android.util.Log;

/**
 * 自定义日志打印工具类
 * 作者：zhuyong on 2019/2/27 17:22
 * 邮箱：99305919@qq.com
 * 希望每天叫醒你的不是闹钟而是梦想
 */
public class LogUtils {
    /**
     * 程序是否在调试阶段
     */
    public static boolean IS_DEBUGING = false;

    // 调试信息：debug
    public static void d(String TAG, String msg) {
        if (IS_DEBUGING) {
            Log.d(TAG, msg);
        }
    }

    // 错误信息:error
    public static void e(String TAG, String msg) {
        if (IS_DEBUGING) {
            Log.e(TAG, msg);
        }
    }

    // 信息：information
    public static void i(String TAG, String msg) {
        if (IS_DEBUGING) {
            Log.i(TAG, msg);
        }
    }

    // 详细信息：verbose
    public static void v(String TAG, String msg) {
        if (IS_DEBUGING) {
            Log.v(TAG, msg);
        }
    }

    // 警告信息：verbose
    public static void w(String TAG, String msg) {
        if (IS_DEBUGING) {
            Log.w(TAG, msg);
        }
    }

}
