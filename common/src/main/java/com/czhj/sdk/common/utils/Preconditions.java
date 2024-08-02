package com.czhj.sdk.common.utils;


import android.os.Looper;

import com.czhj.sdk.logger.SigmobLog;

import java.util.IllegalFormatException;


/**
 * Simple static methods to be called at the start of your own methods to verify correct arguments
 * and state.
 * <p>
 * Each method supports 2 flavors - one that will always throw a runtime exception, and a NoThrow
 * version that will only throw an exception when in strict mode. We use the former
 * for internal state checks, and the later to validate arguments passed to the SDK.
 */
public final class Preconditions {

    private static final String EMPTY_ARGUMENTS = "";

    private Preconditions() {
        // Non-instantiable.
    }

    /**
     * Ensures that an object reference is not null.
     */
    public static void checkNotNull(Object reference) {
        checkNotNullInternal(reference, true, "Object can not be null.", EMPTY_ARGUMENTS);
    }

    /**
     * Preconditions.NoThrow.checks that avoid throwing and exception in release mode. These versions return
     * a boolean which the caller should check.
     */
    public final static class NoThrow {
        private static volatile boolean sStrictMode = true;

        /**
         * Ensures the truth of an expression.
         */
        public static boolean checkArgument(boolean expression) {
            return checkArgumentInternal(expression, sStrictMode, "Illegal argument", EMPTY_ARGUMENTS);
        }

        /**
         * Ensures the truth of an expression, with an error message.
         */
        public static boolean checkArgument(boolean expression, String errorMessage) {
            return checkArgumentInternal(expression, sStrictMode, errorMessage, EMPTY_ARGUMENTS);
        }


        /**
         * Ensures that an object reference is not null.
         */
        public static boolean checkNotNull(Object reference) {
            return checkNotNullInternal(reference, sStrictMode, null, EMPTY_ARGUMENTS);
        }

        /**
         * Ensures that an object reference is not null, with an error message.
         */
        public static boolean checkNotNull(Object reference, String errorMessage) {
            return checkNotNullInternal(reference, sStrictMode, errorMessage, EMPTY_ARGUMENTS);
        }

        public static String getLineInfo() {
            try {

                StackTraceElement ste = new Throwable().getStackTrace()[2];
                return ste.getFileName() + ": Line " + ste.getMethodName();
            } catch (Throwable e) {
                return "";
            }

        }

        /**
         * Ensures that the current thread is the UI thread, with an error message.
         */
        public static boolean checkUiThread(String errorMessage) {
            return checkUiThreadInternal(sStrictMode, errorMessage, EMPTY_ARGUMENTS);
        }

    }

    private static boolean checkArgumentInternal(boolean expression, boolean allowThrow, String errorMessageTemplate, Object... errorMessageArgs) {
        if (expression) {
            return true;
        }


        String errorMessage = format(errorMessageTemplate, errorMessageArgs);
        if (allowThrow) {
            throw new IllegalArgumentException(errorMessage);
        }
        SigmobLog.e(errorMessage);
        return false;
    }

    private static boolean checkStateInternal(boolean expression, boolean allowThrow, String errorMessageTemplate, Object... errorMessageArgs) {
        if (expression) {
            return true;
        }


        String errorMessage = format(errorMessageTemplate, errorMessageArgs);
        if (allowThrow) {
            throw new IllegalStateException(errorMessage);
        }
        SigmobLog.e(errorMessage);
        return false;
    }

    private static boolean checkNotNullInternal(Object reference, boolean allowThrow, String errorMessageTemplate, Object... errorMessageArgs) {
        if (reference != null) {
            return true;
        }


        String errorMessage = format(errorMessageTemplate, errorMessageArgs);
        if (allowThrow) {
            throw new NullPointerException(errorMessage);
        }
        SigmobLog.e(errorMessage);
        return false;
    }

    private static boolean checkUiThreadInternal(boolean allowThrow, String errorMessageTemplate, Object... errorMessageArgs) {
        // Check that the main looper is the current looper.
        if (Looper.getMainLooper().equals(Looper.myLooper())) {
            return true;
        }


        String errorMessage = format(errorMessageTemplate, errorMessageArgs);
        if (allowThrow) {
            throw new IllegalStateException(errorMessage);
        }
        SigmobLog.e(errorMessage);
        return false;
    }

    /**
     * Substitutes each {@code %s} in {@code template} with an argument. These are matched by
     * position - the first {@code %s} gets {@code args[0]}, etc.
     */
    private static String format(String template, Object... args) {
        template = String.valueOf(template);  // null -> "null"

        try {
            return String.format(template, args);
        } catch (IllegalFormatException exception) {
            SigmobLog.e("Sigmob preconditions had a format exception: " + exception.getMessage());
            return template;
        }
    }
}
