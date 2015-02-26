package com.sanderbos.simplephotowebserver;

import android.util.Log;

/**
 * Created by sander on 25-2-2015.
 */
public class MyLog {

    private static final String TAG = "com.sanderbos.photows";

    public static void debug(String message) {
        Log.d(TAG, message);
    }

    public static void info(String message) {
        Log.i(TAG, message);
    }

    public static void warning(String message) {
        Log.w(TAG, message);
    }

    public static void error(String message) {
        Log.e(TAG, message);
    }

    public static void error(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }

}
