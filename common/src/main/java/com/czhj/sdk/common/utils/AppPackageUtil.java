package com.czhj.sdk.common.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.czhj.sdk.logger.SigmobLog;

public class AppPackageUtil {


    private static String packageName;
    private static String versionName;
    private static PackageInfo packageInfo;
    private static String appName;

    public static PackageManager getPackageManager(Context context) {
        if (context != null) {
            return context.getPackageManager();
        }

        return null;
    }

    public static String getAppPackageName(Context context) {
        if (context == null) return null;
        if (!TextUtils.isEmpty(packageName)) {
            return packageName;
        }
        packageName = context.getPackageName();

        return packageName;
    }

    public static String getAppName(Context context) {

        if (context == null) return null;
        if (!TextUtils.isEmpty(appName)) {
            return appName;
        }

        try {
            ApplicationInfo packageInfo = context.getApplicationInfo();
            if (packageInfo != null) {
                appName = context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return appName;
    }

    public static int getPackageVersionCode(Context context, String name) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(name, 0);
            if (packageInfo == null) {
                return -1;
            }
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static PackageInfo getPackageInfo(Context context) {


        if (packageInfo == null) {
            PackageManager packageManager = getPackageManager(context);
            String appPackageName = getAppPackageName(context);
            if (packageManager == null || appPackageName == null) return null;

            try {
                packageInfo = packageManager.getPackageInfo(appPackageName, 0);
            } catch (Throwable e) {

            }
        }
        return packageInfo;

    }

    public static String getAppVersionFromContext(Context context) {
        try {


            if (!TextUtils.isEmpty(versionName)) return versionName;

            PackageInfo packageInfo = getPackageInfo(context);
            if (packageInfo != null) {
                versionName = packageInfo.versionName;
            }

            return versionName;
        } catch (Throwable exception) {
            SigmobLog.d("Failed to retrieve PackageInfo#versionName.");
        }
        return null;
    }
}
