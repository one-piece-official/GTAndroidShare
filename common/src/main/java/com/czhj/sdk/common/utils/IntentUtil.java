package com.czhj.sdk.common.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.czhj.sdk.logger.SigmobLog;
import com.czhj.sdk.common.exceptions.IntentNotResolvableException;

import java.util.List;

public class IntentUtil {
    private IntentUtil() {
    }

    public static void registerReceiver(final Context context, final BroadcastReceiver receiver, IntentFilter intentFilter) {
        registerReceiver(context,receiver,intentFilter,true);
    }
    @SuppressLint("WrongConstant")
    public static void registerReceiver(final Context context, final BroadcastReceiver receiver, IntentFilter intentFilter, boolean isExported){
        if (Build.VERSION.SDK_INT >= 34 && context.getApplicationInfo().targetSdkVersion >= 34 ) {
            context.registerReceiver(receiver, intentFilter, isExported? 2:4);
        }else {
            context.registerReceiver(receiver, intentFilter);
        }
    }
    private static void startActivity(final Context context, final Intent intent)
            throws IntentNotResolvableException {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(intent);

        if (!(context instanceof Activity)) {
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
        } catch (Throwable e) {
            throw new IntentNotResolvableException(e);
        }
    }

    /**
     * Adding FLAG_ACTIVITY_NEW_TASK with startActivityForResult will always result in a
     * RESULT_CANCELED, so don't use it for Activity contexts.
     */
    public static Intent getStartActivityIntent(final Context context,
                                                final Class clazz, final Bundle extras) {
        final Intent intent = new Intent(context, clazz);

        if (!(context instanceof Activity)) {
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        }

        if (extras != null) {
            intent.putExtras(extras);
        }

        return intent;
    }

    public static boolean deviceCanHandleIntent(final Context context, final Intent intent) {
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
//            return true;
//        }
        try {
            final PackageManager packageManager = context.getPackageManager();
            final List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            return !activities.isEmpty();
        } catch (NullPointerException e) {
            return false;
        }
    }


    public static boolean launchApplicationForPackageName(final Context context,
                                                          final String packageName) {
        try {
            Intent shortcutIntent = context.getPackageManager().
                    getLaunchIntentForPackage(packageName);

            if (shortcutIntent != null) {
                startActivity(context, shortcutIntent);
                return true;
            }

        } catch (Throwable throwable) {
            SigmobLog.e(throwable.getMessage());

        }
        return false;
    }


    private static void launchIntentForUserClick(final Context context,
                                                 final Intent intent, final String errorMessage)
            throws IntentNotResolvableException {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(intent);

        try {
            startActivity(context, intent);
        } catch (Throwable e) {
            throw new IntentNotResolvableException(errorMessage + "\n" + e.getMessage());
        }
    }

    public static void launchApplicationUrl(final Context context,
                                            final Uri uri) throws IntentNotResolvableException {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(uri);

        if (deviceCanHandleIntent(context, intent)) {
            launchApplicationIntent(context, intent);
        } else {
            // Deeplink+ needs this exception to know primaryUrl failed and then attempt fallbackUrl
            // See UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK
            throw new IntentNotResolvableException("Could not handle application specific " +
                    "action: " + uri + "\n\tYou may be running in the emulator or another " +
                    "device which does not have the required application.");
        }
    }

    public static void launchApplicationIntent(final Context context,
                                               final Intent intent) throws IntentNotResolvableException {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(intent);

        if (deviceCanHandleIntent(context, intent)) {
            final String errorMessage = "Unable to open intent: " + intent;
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            launchIntentForUserClick(context, intent, errorMessage);
        } else {
//            final String fallbackUrl = intent.getStringExtra("browser_fallback_url");
//            if (TextUtils.isEmpty(fallbackUrl)) {
//                if (!"market".equalsIgnoreCase(intent.getScheme())) {
//                    launchApplicationUrl(context, getPlayStoreUri(intent));
//                } else {
//                    throw new IntentNotResolvableException("Device could not handle neither " +
//                            "intent nor market url.\nIntent: " + intent.toString());
//                }
//            } else {
//                final Uri fallbackUri = Uri.parse(fallbackUrl);
//                launchApplicationUrl(context, fallbackUri);
//            }
        }
    }


    private static Uri getPlayStoreUri(final Intent intent) {
        Preconditions.NoThrow.checkNotNull(intent);

        return Uri.parse("market://details?id=" + intent.getPackage());
    }

    public static void launchActionViewIntent(final Context context,
                                              final Uri uri,
                                              final String errorMessage) throws IntentNotResolvableException {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(uri);

        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (!(context instanceof Activity)) {
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        }
        launchIntentForUserClick(context, intent, errorMessage);
    }

}
