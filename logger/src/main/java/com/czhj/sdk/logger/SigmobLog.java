package com.czhj.sdk.logger;

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SigmobLog {
    private static final String LOGGER_NAMESPACE = "com.sigmob";

    private static final String LOGTAG = "sigmob";
    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAMESPACE);
    private static final boolean DEBUG = true;
    private static final SigmobLogHandler LOG_HANDLER = new SigmobLogHandler();

    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);
        LOG_HANDLER.setLevel(Level.INFO);
        LogManager.getLogManager().addLogger(LOGGER);
        addHandler(LOGGER, LOG_HANDLER);
    }

    private SigmobLog() {

    }

    public static void c(final String message) {
        SigmobLog.c(message, null);
    }

    public static void v(final String message) {
        SigmobLog.v(message, null);
    }

    public static void d(final String message) {
        if (DEBUG) {
            SigmobLog.d(message, null);
        }
    }

    /**
     * 截断输出日志
     **/
    public static void dd(String tag, String msg) {
        if (tag == null || tag.isEmpty() || msg == null || msg.isEmpty()) return;
        int segmentSize = 3 * 1024;
        long length = msg.length();
        if (length <= segmentSize) {// 长度小于等于限制直接打印
            Log.d(tag, msg);
        } else {
            while (msg.length() > segmentSize) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize);
                msg = msg.replace(logContent, "");
                Log.d(tag, logContent);
            }
            Log.d(tag, msg);// 打印剩余日志
        }
    }

    public static void i(final String message) {
        SigmobLog.i(message, null);
    }

    public static void w(final String message) {
        SigmobLog.w(message, null);
    }

    public static void e(final String message) {
        if (!TextUtils.isEmpty(message)) {
            if (DEBUG) {
                SigmobLog.e(message, new Throwable(message));
            } else {
                SigmobLog.e(message, null);

            }
        }
    }

    private static void c(final String message, final Throwable throwable) {
        LOGGER.log(Level.FINEST, message, throwable);
    }

    public static void v(final String message, final Throwable throwable) {
        LOGGER.log(Level.FINE, message, throwable);
    }

    public static void d(final String message, final Throwable throwable) {
        if (DEBUG) {
            LOGGER.log(Level.CONFIG, message, throwable);
        }
    }

    private static void i(final String message, final Throwable throwable) {
        LOGGER.log(Level.INFO, message, throwable);
    }

    public static void w(final String message, final Throwable throwable) {
        LOGGER.log(Level.WARNING, message, throwable);
    }

    public static void e(final String message, final Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }


    public static void setSdkHandlerLevel(final Level level) {
        LOG_HANDLER.setLevel(level);
    }

    /**
     * Adds a {@link Handler} to a {@link Logger} if they are not already associated.
     */
    private static void addHandler(final Logger logger, final Handler handler) {
        final Handler[] currentHandlers = logger.getHandlers();
        for (final Handler currentHandler : currentHandlers) {
            if (currentHandler.equals(handler)) {
                return;
            }
        }
        logger.addHandler(handler);
    }


    /**
     * Adds a {@link Handler} to a {@link Logger} if they are not already associated.
     */
    public static void addHandler(final Handler handler) {

        addHandler(LOGGER, handler);
    }

    private static final class SigmobLogHandler extends Handler {
        private static final Map<Level, Integer> LEVEL_TO_LOG = new HashMap<>(7);

        /*
         * Mapping between Level.* and Log.*:
         * Level.FINEST  => Log.v
         * Level.FINER   => Log.v
         * Level.FINE    => Log.v
         * Level.CONFIG  => Log.d
         * Level.INFO    => Log.i
         * Level.WARNING => Log.w
         * Level.SEVERE  => Log.e
         */
        static {
            LEVEL_TO_LOG.put(Level.FINEST, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.FINER, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.FINE, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.CONFIG, Log.DEBUG);
            LEVEL_TO_LOG.put(Level.INFO, Log.DEBUG);
            LEVEL_TO_LOG.put(Level.WARNING, Log.WARN);
            LEVEL_TO_LOG.put(Level.SEVERE, Log.ERROR);
        }

        @Override
        @SuppressWarnings({"LogTagMismatch", "WrongConstant"})
        public void publish(final LogRecord logRecord) {
            if (isLoggable(logRecord)) {
                final int priority;
                if (LEVEL_TO_LOG.containsKey(logRecord.getLevel())) {
                    priority = LEVEL_TO_LOG.get(logRecord.getLevel());
                } else {
                    priority = Log.VERBOSE;
                }

                String message = logRecord.getMessage() + "\n";

                final Throwable error = logRecord.getThrown();
                if (error != null) {
                    message += Log.getStackTraceString(error);
                }

                Log.println(priority, LOGTAG, message);
            }
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }
    }
}
