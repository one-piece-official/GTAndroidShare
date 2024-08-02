package com.czhj.sdk.common.utils;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.net.ConnectivityManager.TYPE_ETHERNET;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.czhj.sdk.logger.SigmobLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class DeviceUtils {

    final static String[] emulatorFiles = new String[]{
            "/system/lib/libdroid4x.so", "/system/bin/mount.vboxsf", "/system/lib/vboxguest.ko", "/etc/mumu-configs",
            "/system/lib/vboxsf.ko", "/system/lib/vboxvideo.ko", "/data/.bluestacks.prop", "/system/bin/microvirt-vbox-sf", "/system/lib/tboxsf.ko", "/system/bin/androVM-vbox-sf",
            "/system/bin/microvirtd", "/system/bin/windroyed", "/system/lib/libdroid4x.so"
    };
    private static final int MAX_MEMORY_CACHE_SIZE = 30 * 1024 * 1024; // 30 MB
    private static final String SIM_STATE = "getSimState";
    private static final String SIM_IMEI = "getImei";
    private static final String SIM_LINE_NUMBER = "getLine1Number";
    private static final int MIN_DISK_CACHE_SIZE = 30 * 1024 * 1024; // 30 MB
    private static final int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final int UNKNOWN_NETWORK = -1;
    private static String mMacAddress;
    private static int isEmulator = 0;
    private static String mBssid;
    private static String mWifiName;
    private static String mAndroidId;
    private static String bluetooth_name;
    private static NetworkType mNetworkType = NetworkType.UNKNOWN;
    private static boolean misNetworkConnected;
    private static long retryIMEI;
    private static long retryIMSI;
    private static long retryLocation;
    private static long retryWIFI;
    private static ArrayList<Network> availableNetworks = new ArrayList();
    private static Network mLostNetwork;
    private static long totalMem;
    private static String apkSha1;
    private static String apkMd5;

    private static String mOperatorName;
    private static String mOperator;
    private static Display mDefaultDisplay;

    public static int memoryCacheSizeBytes(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return 0;
        long memoryClass = activityManager.getMemoryClass();

        try {
            final int flagLargeHeap = ApplicationInfo.class.getDeclaredField("FLAG_LARGE_HEAP").getInt(null);
            if (bitMaskContainsFlag(context.getApplicationInfo().flags, flagLargeHeap)) {
                memoryClass = (Integer) new ReflectionUtil.MethodBuilder(activityManager, "getLargeMemoryClass").execute();
            }
        } catch (Throwable e) {
            SigmobLog.d("Unable to reflectively determine large heap size.");
        }

        long result = Math.min(MAX_MEMORY_CACHE_SIZE, memoryClass / 8 * 1024 * 1024);
        return (int) result;
    }

    public static String getSDCardPath(Context context) {


        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            List<StorageVolume> storageVolumeList = mStorageManager.getStorageVolumes();
            for (StorageVolume volume : storageVolumeList) {
                if (volume.isRemovable()) {
                    try {
                        Method getPath = volume.getClass().getMethod("getPath", new Class[0]);
                        String path = (String) getPath.invoke(volume, new Object[0]);

                        return path;
                    } catch (Exception e) {
                        SigmobLog.e(e.getMessage());
                    }
                }
            }
        } else {

            try {

                //得到StorageManager中的getVolumeList()方法的对象
                final Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
                //---------------------------------------------------------------------

                //得到StorageVolume类的对象
                final Class<?> storageValumeClazz = Class.forName("android.os.storage.StorageVolume");
                //---------------------------------------------------------------------
                //获得StorageVolume中的一些方法
                final Method getPathMethod = storageValumeClazz.getMethod("getPath");
                Method isRemovableMethod = storageValumeClazz.getMethod("isRemovable");

                Method mGetState = null;
                //getState 方法是在4.4_r1之后的版本加的，之前版本（含4.4_r1）没有

                //调用getVolumeList方法，参数为：“谁”中调用这个方法
                final Object invokeVolumeList = getVolumeList.invoke(mStorageManager);
                final int length = Array.getLength(invokeVolumeList);

                for (int i = 0; i < length; i++) {
                    try {
                        final Object storageValume = Array.get(invokeVolumeList, i);//得到StorageVolume对象
                        Object isRemovable = isRemovableMethod.invoke(storageValume);
                        if (isRemovable instanceof Boolean && ((Boolean) isRemovable).booleanValue()) {
                            Object pathObject = getPathMethod.invoke(storageValume);
                            if (pathObject instanceof String) {
                                return (String) pathObject;
                            }
                        }
                    } catch (Throwable throwable) {

                    }
                }
            } catch (Throwable throwable) {

            }
        }
        return null;
    }

    public static long diskCacheSizeBytes(File dir, long minSize) {
        long size = minSize;
        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long availableBytes = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            size = availableBytes / 50;
        } catch (IllegalArgumentException e) {
            SigmobLog.d("Unable to calculate 2% of available disk space, defaulting to minimum");
        }

        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
    }

    public static WindowManager getWindowManger(Context context) {
        return (WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE);
    }

    public static String getRotation(Context context) {


        final Display display = getDisplay(context);
        if (display == null) return "0";
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                return "0";
            case Surface.ROTATION_180:
                return "180";
            case Surface.ROTATION_270:
                return "270";
            case Surface.ROTATION_90:
                return "90";
            default:
                return "0";
        }

    }

    public static long getBootSystemTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

    /**
     * 获取apk sha1 md5
     */
    public static String getApkSha1OrMd5(Context context, String name) {

        if (TextUtils.isEmpty(name)) return null;
        if (name.equals("SHA1") && apkSha1 != null) {
            return apkSha1;
        } else if (name.equals("MD5") && apkMd5 != null) {
            return apkMd5;
        }
        String result = null;

        try {
            PackageManager localPackageManager = context.getPackageManager();
            PackageInfo localPackageInfo = localPackageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            if ((localPackageInfo != null) && (localPackageInfo.signatures != null) && (localPackageInfo.signatures.length > 0)) {
                android.content.pm.Signature localSignature = localPackageInfo.signatures[0];
                if (localSignature != null) {
                    byte[] byteArray = localSignature.toByteArray();
                    MessageDigest instance = MessageDigest.getInstance(name);
                    if (instance != null) {
                        byte[] digest = instance.digest(byteArray);
                        StringBuilder sb = new StringBuilder();
                        for (byte b : digest) {
                            sb.append(Integer.toHexString((b & 255) | 256).substring(1, 3).toUpperCase());
                            sb.append(":");
                        }
                        result = sb.substring(0, sb.length() - 1);
                        if (name.equals("SHA1")) {
                            apkSha1 = result;
                        } else if (name.equals("MD5")) {
                            apkMd5 = result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            SigmobLog.e(e.getMessage());
        }

        return result;

    }

    public static String getDeviceDispaly() {
        return Build.DISPLAY;
    }

    public static String getCPUInfo() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return Build.SUPPORTED_ABIS[0];
            } else {
                return Build.CPU_ABI;
            }
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        return null;

    }

    public static String getProperty(String propName) {
        String value = null;
        Object roSecureObj;
        try {
            roSecureObj = Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class)
                    .invoke(null, propName);
            if (roSecureObj != null) value = (String) roSecureObj;
        } catch (Throwable e) {

        }
        return value;

    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static String getDeviceType(Context context) {
        return isTablet(context) ? "pad" : "phone";
    }

    public static WifiManager getWifiManager(Context context) {
        if (context != null && isCanUseWifiState(context)) {
            return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
        return null;
    }


    public static boolean isCanUseWifiState(Context context) {
        boolean result = context.checkCallingOrSelfPermission(ACCESS_WIFI_STATE) == PERMISSION_GRANTED;
        SigmobLog.d("isCanUseWifiState status " + result);
        return result;
    }

    public static boolean isCanUseLocation(Context context) {
        boolean result = context.checkCallingOrSelfPermission(ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED
                || context.checkCallingOrSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED;

        SigmobLog.d("isCanUseLocation status " + result);
        return result;
    }

    /**
     * @return the display orientation. Useful when generating ad requests.
     */
    public static int getOrientationInt(Context context) {
        final int orientationInt = context.getResources().getConfiguration().orientation;

        return orientationInt;
    }

    /**
     * @return the display orientation. Useful when generating ad requests.
     */
    public static float getBatteryLevel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager == null) return 0.f;
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100.0f;

        }
        return 0.f;

    }

//    /**
//     * 获取设备的唯一标识，deviceId
//     *
//     * @return
//     */
//    @SuppressLint("MissingPermission")
//    public static String getDeviceId(Context context) {
//        try {
//            return DeviceHelper.getIMEI(context);
//        } catch (Throwable t) {
//
//        }
//        return null;
//    }
//
//    /**
//     * 获取设备的唯一标识，deviceId
//     *
//     * @return
//     */
//    public static String getDeviceId(Context context, int index) {
//
//        try {
//            return DeviceHelper.getIMEI(context, index);
//        } catch (Throwable t) {
//
//        }
//        return null;
//    }

    public static boolean getBatterySaveEnable(Context context) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager == null) return false;

            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < 16;
        } else {
            return false;
        }
    }

    /**
     * @return * 电池充电的状态（0=UnKnow、1=Unplugged、2=Charging、3=Full）
     */
    public static int getBatteryState(Context context) {
        BatteryManager batteryManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

            if (batteryManager == null) return 0;
            int status = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
            }

            switch (status) {
                case BatteryManager.BATTERY_STATUS_FULL:
                    return 3;
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    return 2;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    return 1;
                default:
                    return 0;
            }
        }
        return 0;
    }

    /**
     * 获取系统内存大小
     *
     * @return
     */
    public static long getSysteTotalMemorySize(Context context) {

        try {
            //获得ActivityManager服务的对象

            if (totalMem > 0) return totalMem;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

                ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                //获得MemoryInfo对象
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                //获得系统可用内存，保存在MemoryInfo对象上
                if (mActivityManager != null) {
                    mActivityManager.getMemoryInfo(memoryInfo);
                    totalMem = memoryInfo.totalMem;
                    return totalMem;
                }
            }

        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }

        return 0;

    }

    /**
     * Get the logical density of the display as in
     */
    public static float getDensityDpi(Context context) {
        try {
            return getRealMetrics(context).densityDpi;
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        return 0;
    }

    public static Locale getDeviceLocale(Context context) {
        try {
            return context.getResources().getConfiguration().locale;
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        return null;
    }

//    public static String getSimPhonenumber(Context context, int slotIdx) {
//        try {
//            return (String) getSimByMethod(context, SIM_LINE_NUMBER, getSubidBySlotId(context, slotIdx));
//
//        } catch (Throwable t) {
//
//        }
//        return null;
//
//    }

    public static String getBlueToothName(Context context) {

        if (TextUtils.isEmpty(bluetooth_name)) {
            try {
                bluetooth_name = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name");
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }

        }

        return bluetooth_name;

    }

    public static String getDeviceName(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                return Settings.Global.getString(context.getContentResolver(), "device_name");
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
        } else {
            return getBlueToothName(context);
        }
        return null;
    }

//    @SuppressLint("NewApi")
//    public static int getSubidBySlotId(Context context, int slotId) {
//
//        try {
//            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(
//                    Context.TELEPHONY_SUBSCRIPTION_SERVICE);
//
//            Class<?> telephonyClass = Class.forName(subscriptionManager.getClass().getName());
//            Class<?>[] parameter = new Class[1];
//            parameter[0] = int.class;
//            Method getSimState = telephonyClass.getMethod("getSubId", parameter);
//            Object[] obParameter = new Object[1];
//            obParameter[0] = slotId;
//            Object ob_phone = getSimState.invoke(subscriptionManager, obParameter);
//
//            if (ob_phone != null) {
//                SigmobLog.d("slotId:" + slotId + ";" + ((int[]) ob_phone)[0]);
//                return ((int[]) ob_phone)[0];
//            }
//        } catch (Throwable e) {
//            SigmobLog.e("getSubidBySlotId: ", e);
//        }
//
//
//        return -1;
//
//    }

    public static int getAppLaunchCount(Context context, String packageName) {
        int aLaunchCount = 0;
        PackageManager pm = context.getPackageManager();

        try {
            Intent i = pm.getLaunchIntentForPackage(packageName);
            SigmobLog.d("getAppLaunchCount==" + packageName);
            if (i == null)
                return 0;
            ComponentName aName = i.getComponent();
            SigmobLog.d("getAppLaunchCount==" + packageName);
            // 隐藏引用
            // 获得ServiceManager类
            Class ServiceManager = Class.forName("android.os.ServiceManager");
            // 获得ServiceManager的getService方法
            Method getService = ServiceManager.getMethod("getService", java.lang.String.class);
            // 调用getService获取RemoteService
            Object oRemoteService = getService.invoke(null, "usagestats");
            // 获得IUsageStats.Stub类
            Class cStub = Class.forName("com.android.internal.app.IUsageStats$Stub");
            // 获得asInterface方法
            Method asInterface = cStub.getMethod("asInterface", android.os.IBinder.class);
            // 调用asInterface方法获取IUsageStats对象
            Object oIUsageStats = asInterface.invoke(null, oRemoteService);
            // 获得getPkgUsageStats(ComponentName)方法
            Method getPkgUsageStats = oIUsageStats.getClass().getMethod("getPkgUsageStats", ComponentName.class);
            // 调用getPkgUsageStats 获取PkgUsageStats对象
            Object aStats = getPkgUsageStats.invoke(oIUsageStats, aName);
            SigmobLog.d("getAppLaunchCount==" + packageName);
            if (aStats == null)
                return 0;
            // 获得PkgUsageStats类
            Class PkgUsageStats = Class.forName("com.android.internal.os.PkgUsageStats");
            SigmobLog.d("getAppLaunchCount==" + packageName);
            aLaunchCount = PkgUsageStats.getDeclaredField("launchCount").getInt(aStats);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return aLaunchCount;
    }

    /**
     * 判断手机是否root
     */
    public static boolean isRoot() {
        String binPath = "/system/bin/su";
        String xBinPath = "/system/xbin/su";
        return new File(binPath).exists() && isExecutable(binPath) || new File(xBinPath).exists() && isExecutable(xBinPath);
    }

    private static boolean isExecutable(String filePath) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("ls -l " + filePath);
            // 获取返回内容
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            String str = in.readLine();
            if (str != null && str.length() >= 4) {
                char flag = str.charAt(3);
                if (flag == 's' || flag == 'x')
                    return true;
            }
        } catch (Throwable e) {
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return false;
    }

    public static Display getDisplay(Context context) {
        if (mDefaultDisplay == null) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            if (windowManager == null) return null;

            mDefaultDisplay = windowManager.getDefaultDisplay();
        }

        return mDefaultDisplay;
    }

    public static DisplayMetrics getRealMetrics(Context context) {

        DisplayMetrics dm = new DisplayMetrics();
        Display display = getDisplay(context);

        if (display == null) return context.getResources().getDisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(dm);
        } else {
            @SuppressWarnings("rawtypes")
            Class c;
            try {
                c = Class.forName("android.view.Display");
                @SuppressWarnings("unchecked")
                Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
                method.invoke(display, dm);
            } catch (Exception e) {
                SigmobLog.e(e.getMessage());
            }
        }
        return dm;

    }

    public static ConnectivityManager getConnectivityManager(Context context) {
        if (context != null) {
            return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        return null;
    }

    private static boolean isCanUseConnection(Context context) {
        int result = context.checkCallingOrSelfPermission(Manifest.permission.INTERNET);
        return result == PERMISSION_GRANTED;

    }

    public static boolean isNetworkValid(NetworkCapabilities capabilities) {
        if (capabilities != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                boolean validated = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                }
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                        || validated;

            }
        }
        return false;
    }


    @SuppressLint("MissingPermission")
    public static void updateNetworkType(Context context) {
        try {
            Network activeNetwork = null;
            if (!isCanUseConnection(context)) return;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                activeNetwork = getConnectivityManager(context).getActiveNetwork();

                NetworkCapabilities networkCapabilities = null;

                if (activeNetwork != null && activeNetwork != mLostNetwork) {
//                    SigmobLog.d(" updateNetworkType activeNetwork " + activeNetwork);
                    networkCapabilities = getConnectivityManager(context).getNetworkCapabilities(activeNetwork);
                } else {
//                    SigmobLog.d(" updateNetworkType activeNetwork is null");
                    for (int i = availableNetworks.size() - 1; i >= 0; i--) {
                        activeNetwork = availableNetworks.get(i);
                        networkCapabilities = getConnectivityManager(context).getNetworkCapabilities(activeNetwork);
                        if (networkCapabilities != null) {
                            break;
                        }
                    }
                }
                if (networkCapabilities != null) {
//                    SigmobLog.d("updateNetworkType " + networkCapabilities);
                    misNetworkConnected = isNetworkValid(networkCapabilities);
//                    SigmobLog.d("updateNetworkType misNetworkConnected " + misNetworkConnected);

                    if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {

                        mNetworkType = NetworkType.WIFI;
                    } else if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        mNetworkType = getDataNetworkType(context);
                    } else {
                        mNetworkType = NetworkType.UNKNOWN;
                    }

                }

            } else {
                NetworkInfo activeNetworkInfo = getConnectivityManager(context).getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    misNetworkConnected = activeNetworkInfo.isAvailable();
                }
                int networkType = activeNetworkInfo != null
                        ? activeNetworkInfo.getType() : UNKNOWN_NETWORK;
                mNetworkType = NetworkType.fromAndroidNetworkType(context, networkType);

            }

        } catch (Exception e) {
            //TODO: handle exception
        }

    }

    public static NetworkType getActiveNetworkType() {

        return mNetworkType;
    }

    public static boolean isCanUsePhoneState(Context context) {
        boolean result = context.checkCallingOrSelfPermission(READ_PHONE_STATE) == PERMISSION_GRANTED;
//        SigmobLog.d("isCanUsePhoneState status " + result);
        return result;

    }

    public static boolean isCanUseWriteExternal(Context context) {
        boolean result = context.checkCallingOrSelfPermission(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;

        SigmobLog.d("isCanUseWriteExternal status " + result);

        return result;
    }

    public static void resetRetryIMEI() {
        retryIMEI = 0;
    }

    public static boolean isCanRetryIMEI() {

        boolean result = System.currentTimeMillis() - retryIMEI > 30000;
        if (result) {
            retryIMEI = System.currentTimeMillis();
        }

        SigmobLog.d("isCanRetryIMEI status " + result);

        return result;
    }


    public static boolean isCanRetryWIFI() {

        boolean result = System.currentTimeMillis() - retryWIFI > 30000;
        if (result) {
            retryWIFI = System.currentTimeMillis();
        }
        SigmobLog.d("isCanRetryWIFI status " + result);
        return result;
    }

    public static boolean isCanRetryLocation() {

        boolean result = System.currentTimeMillis() - retryLocation > 36000;
        if (result) {
            retryLocation = System.currentTimeMillis();
        }

        SigmobLog.d("isCanRetryLocation status " + result);

        return result;
    }


    public static TelephonyManager getTelephonyManager(Context context) {

        if (context == null) return null;

        try {
            if (!DeviceUtils.isCanUsePhoneState(context)) {
                return null;
            }

        } catch (Throwable th) {

        }


        return (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public static String getNetworkOperator(Context context) {
        if (mOperator != null) {
            return mOperator;
        }
        final TelephonyManager telephonyManager = getTelephonyManager(context);

        if (telephonyManager != null) {

            if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA &&
                    telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
                mOperator = telephonyManager.getSimOperator();
            } else {
                mOperator = telephonyManager.getNetworkOperator();
            }
            if (mOperator == null) {
                mOperator = "";
            }
        }
        return mOperator;
    }

    public static String getNetworkOperatorName(Context context) {

        if (mOperatorName != null) {
            return mOperatorName;
        }

        final TelephonyManager telephonyManager = getTelephonyManager(context);

        if (telephonyManager != null) {

            if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA &&
                    telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
                mOperatorName = telephonyManager.getSimOperatorName();
            } else {
                mOperatorName = telephonyManager.getNetworkOperatorName();
            }

            if (mOperatorName == null) {
                mOperatorName = "";
            }
        }
        return mOperatorName;
    }

    public static boolean isNetworkConnected() {
        return misNetworkConnected;
    }


    /**
     * @return the network operator for URL generators.
     */
    public static String getNetworkOperatorForUrl(Context context) {
        return getNetworkOperator(context);
    }

    @SuppressLint("MissingPermission")
    public static NetworkType getDataNetworkType(Context context) {
        SigmobLog.d("getDataNetworkType ");

        TelephonyManager telephonyManager = getTelephonyManager(context);
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkType = telephonyManager.getDataNetworkType();
            } else {
                try {
                    networkType = telephonyManager.getNetworkType();
                } catch (Exception ex) {
                }
            }
        }

        ConnectivityManager connectivityManager = getConnectivityManager(context);

        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    networkType = networkInfo.getSubtype();
                }
            }
        }
        SigmobLog.d("getDataNetworkType " + networkType);


        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NetworkType.MOBILE_2G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NetworkType.MOBILE_3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NetworkType.MOBILE_4G;
            case TelephonyManager.NETWORK_TYPE_NR:
                return NetworkType.MOBILE_5G;
            default:
                return NetworkType.MOBILE;
        }
    }

    @SuppressLint("MissingPermission")
    public static void registerNetworkChange(final Context context) {
        final NetworkRequest networkRequest;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            IntentUtil.registerReceiver(context, new NetBroadcastReceiver(), new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        } else {
            networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).
                    addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();

            ConnectivityManager connectivityManager = getConnectivityManager(context);
            if (connectivityManager == null) return;

            connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {


                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                    updateNetworkType(context);
                }


                @Override
                public void onAvailable(Network network) {
                    SigmobLog.d("updateNetworkType registerNetworkCallback  onAvailable " + network.hashCode());
                    super.onAvailable(network);
                    availableNetworks.add(network);
                    updateNetworkType(context);
                }

                @Override
                public void onLost(Network network) {
                    SigmobLog.d("updateNetworkType registerNetworkCallback onLost" + network.hashCode());
                    super.onLost(network);

                    mNetworkType = NetworkType.UNKNOWN;
                    misNetworkConnected = false;
                    try {
                        mLostNetwork = network;
                        availableNetworks.remove(network);
                        updateNetworkType(context);
                    } catch (Throwable th) {

                    }


                }
            });
        }
    }

    public static String getAndroidId(Context context) {
        if (mAndroidId == null && context != null) {
            try {
                SigmobLog.d("private : AndroidId");
                mAndroidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Throwable e) {
                mAndroidId = "";
                SigmobLog.e(e.getMessage());
                mAndroidId = "";
            }
        }

        return mAndroidId;
    }

    public static String getDeviceBrand() {
        return Build.BRAND;
    }

    public static String getDeviceSerial() {
        return Build.SERIAL;
    }

    public static int getDeviceOSLevel() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * @return the device manufacturer.
     */
    public static String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * @return the device model identifier.
     */
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    /**
     * @return
     */
    public static String getCell_ip() {

        try {
            //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface inter = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddress = inter.getInetAddresses(); enumIpAddress.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddress.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {//获取IPv4的IP地址
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0.0";
    }

    /**
     * @return the device os version.
     */
    public static String getDeviceOsVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * @return the device screen width in dips according to current orientation.
     */
    public static int getDeviceScreenWidthDip(Context context) {
        return Dips.screenWidthAsIntDips(context);
    }

    public static int getDeviceScreenRealWidthDip(Context context) {
        if (context == null) return 0;
        return Dips.pixelsToIntDips(getRealMetrics(context).widthPixels, context);
    }

    /**
     * @return the device screen height in dips according to current orientation.
     */
    public static int getDeviceScreenHeightDip(Context context) {
        if (context == null) return 0;
        return Dips.screenHeightAsIntDips(context);
    }

    public static int getDeviceScreenRealHeightDip(Context context) {
        if (context == null) return 0;
        return Dips.pixelsToIntDips(getRealMetrics(context).heightPixels, context);
    }

    public static DisplayMetrics getDisplayMetrics(Context context) {
        try {
            return context.getResources().getDisplayMetrics();
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        return null;
    }

    private static boolean bitMaskContainsFlag(final int bitMask, final int flag) {
        return (bitMask & flag) != 0;
    }

    public static boolean isEmulator() {


        if (isEmulator > 0) {

            return isEmulator > 3;
        }

        try {

            String baseBandVersion = getProperty("gsm.version.baseband");
            if (TextUtils.isEmpty(baseBandVersion) || baseBandVersion.contains("1.0.0.0"))
                ++isEmulator;//基带信息

            String buildFlavor = getProperty("ro.build.flavor");
            if (TextUtils.isEmpty(buildFlavor))
                ++isEmulator;//基带信息
            else if (buildFlavor.contains("vbox") || buildFlavor.contains("sdk_gphone")) {
                isEmulator += 10;

            }

            String productBoard = getProperty("ro.product.board");
            if (TextUtils.isEmpty(productBoard))
                ++isEmulator;//芯片
            else if (productBoard.contains("android") || productBoard.contains("goldfish")) {
                isEmulator += 10;
            }

            String boardPlatform = getProperty("ro.board.platform");
            if (TextUtils.isEmpty(boardPlatform) || boardPlatform.contains("android"))
                ++isEmulator;//芯片平台

            if (TextUtils.isEmpty(Build.BRAND) || Build.BRAND.contains("android"))
                ++isEmulator;//芯片平台

            String hardWare = getProperty("ro.hardware");
            if (null == hardWare) ++isEmulator;
            else if (hardWare.toLowerCase().contains("ttvm")) isEmulator += 10;//天天
            else if (hardWare.toLowerCase().contains("nox")) isEmulator += 10;//夜神


            for (String file : emulatorFiles) {
                if (new File(file).exists()) {
                    SigmobLog.e("find emulator " + file);
                    isEmulator += 10;
                }
            }

        } catch (Throwable throwable) {

        }

        return isEmulator > 3;


    }

    public enum NetworkType {
        UNKNOWN(0),
        ETHERNET(101),
        WIFI(100),
        MOBILE(1),
        MOBILE_2G(2),
        MOBILE_3G(3),
        MOBILE_4G(4),
        MOBILE_5G(5);


        private final int mId;

        NetworkType(int id) {
            mId = id;
        }

        private static NetworkType fromAndroidNetworkType(Context context, int type) {
            switch (type) {
                case TYPE_ETHERNET:
                    return ETHERNET;
                case ConnectivityManager.TYPE_WIFI:
                    return WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_MOBILE_DUN:
                case ConnectivityManager.TYPE_MOBILE_HIPRI:
                case ConnectivityManager.TYPE_MOBILE_MMS:
                case ConnectivityManager.TYPE_MOBILE_SUPL:
                    return getDataNetworkType(context);
                default:
                    return UNKNOWN;
            }
        }

        @Override
        public String toString() {
            return Integer.toString(mId);
        }

        public int getId() {
            return mId;
        }
    }

    public static class NetBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                updateNetworkType(context);
            }
        }
    }
}
