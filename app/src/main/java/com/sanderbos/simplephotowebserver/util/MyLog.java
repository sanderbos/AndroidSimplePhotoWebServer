package com.sanderbos.simplephotowebserver.util;

import android.os.Message;
import android.util.Log;

import java.text.MessageFormat;

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
     * DEbug info message.
     *
     * @param messageTemplate The message template in MessageFormat style.
     * @param arguments The arguments for the debug message.
     */
    public static void debug(String messageTemplate, Object... arguments) {
        debug(MessageFormat.format(messageTemplate, arguments));
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
     * Log info message.
     *
     * @param messageTemplate The message template in MessageFormat style.
     * @param arguments The arguments for the info message.
     */
    public static void info(String messageTemplate, Object... arguments) {
        info(MessageFormat.format(messageTemplate, arguments));
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

    /**
     * Log an error message, with a template and arguments.
     * @param messageTemplate The message template in MessageFormat style.
     * @param arguments The arguments for the error message.
     */
    public static void error(String messageTemplate, Object... arguments) {
        error(MessageFormat.format(messageTemplate, arguments));
    }
}
