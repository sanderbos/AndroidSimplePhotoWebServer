package com.sanderbos.simplephotowebserver.util;

import android.util.Log;

/**
 * Simple logging wrapper to allow for easy extension.
 */
public class MyLog {

    /**
     * Tag used for all log messages.
     */
    private static final String TAG = "com.sanderbos.photows";

    /**
     * Log debug message.
     *
     * @param message The message to log.
     */
    public static void debug(String message) {
        Log.d(TAG, message);
    }

    /**
     * Log info message.
     *
     * @param message The message to log.
     */
    public static void info(String message) {
        Log.i(TAG, message);
    }

    /**
     * Log warning message.
     *
     * @param message The message to log.
     */
    public static void warning(String message) {
        Log.w(TAG, message);
    }

    /**
     * Log error message.
     *
     * @param message The message to log.
     */
    public static void error(String message) {
        Log.e(TAG, message);
    }

    /**
     * Log error message.
     *
     * @param message   The message to log.
     * @param throwable The exception to log.
     */
    public static void error(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }

}
