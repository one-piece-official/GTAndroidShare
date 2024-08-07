package com.czhj.sdk.common;


import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowInsets;

import com.czhj.devicehelper.DeviceHelper;
import com.czhj.devicehelper.oaId.helpers.DevicesIDsHelper;
import com.czhj.sdk.common.Database.SQLiteMTAHelper;
import com.czhj.sdk.common.Database.SQLiteTrackHelper;
import com.czhj.sdk.common.ThreadPool.RepeatingHandlerRunnable;
import com.czhj.sdk.common.ThreadPool.ThreadPoolFactory;
import com.czhj.sdk.common.models.Config;
import com.czhj.sdk.common.mta.BuriedPointManager;
import com.czhj.sdk.common.network.SigmobRequestUtil;
import com.czhj.sdk.common.track.TrackManager;
import com.czhj.sdk.common.utils.AESUtil;
import com.czhj.sdk.common.utils.AdvertisingId;
import com.czhj.sdk.common.utils.AppPackageUtil;
import com.czhj.sdk.common.utils.DeviceUtils;
import com.czhj.sdk.common.utils.IdentifierManager;
import com.czhj.sdk.common.utils.RomUtils;
import com.czhj.sdk.common.utils.SharedPreferencesUtil;
import com.czhj.sdk.logger.SigmobLog;
import com.tan.mark.TanId;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class ClientMetadata implements IdentifierManager.AdvertisingIdChangeListener {
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    private static String mUserId = "-1";
    private static String bootId;
    /**
     * 0=无法探测当前网络状态; 1=蜂窝数据接入，未知网络类型; 2=2G; 3=3G; 4=4G; 5=5G; 100=Wi-Fi网络接入; 101=以太网接入
     */
    private static volatile ClientMetadata sInstance;
    // Network type constant defined after API 9:
    private Location mLocation;
    private IdentifierManager mIdentifierManager;
    private int mInsetBottom;
    private boolean mIsRetryAble = true;
    private String mImei;
    private String mImeiIndex0;
    private String mImeiIndex1;
    private String updateId;
    private Context mContext;
    private boolean enableLocation;

    private String mOaid;
    private String mOAID_SDK;

    private String mOAID_API;
    private String targetSdkVersion;
    private long install_time;
    private int mDeviceHeight;
    private int mDeviceWidth;
    private Display mDefaultDisplay;
    private RepeatingHandlerRunnable repeatingHandlerRunnable;
    private boolean oaidSDKCallbacked;

    public static String getUserId() {
        return TextUtils.isEmpty(mUserId) ? "-1" : mUserId;
    }

    public static void setUserId(String userId) {
        if (!TextUtils.isEmpty(userId)) {
            mUserId = userId;
        }
    }

    public static String getDeviceOsVersion() {
        return DeviceUtils.getDeviceOsVersion();
    }

    public static String getDeviceManufacturer() {
        return DeviceUtils.getDeviceManufacturer();
    }

    public static String getDeviceModel() {
        return DeviceUtils.getDeviceModel();
    }

    public static Long getBuildTime() {
        return DeviceUtils.getBuildTime();
    }

    public static long getRebootTime() {
        long rebootTime = new Date().getTime() - SystemClock.elapsedRealtime();
        return new Date().getTime() - SystemClock.elapsedRealtime();
    }

    public static String getCell_ip() {
        return DeviceUtils.getCell_ip();
    }

    public static Long getBootSystemTime() {
        return DeviceUtils.getBootSystemTime();
    }

    public static Integer getDeviceOSLevel() {
        return DeviceUtils.getDeviceOSLevel();
    }

    public static String getMacAddress() {
        try {
            return DeviceUtils.getMacAddress();
        } catch (Throwable ignored) {

        }
        return "";
    }

    public static String getCPUInfo() {
        try {
            return DeviceUtils.getCPUInfo();
        } catch (Throwable ignored) {

        }
        return null;
    }

    public static String getCPUModel() {
        return Build.BOARD;
    }

    public static String getVAID() {
        try {
            return DeviceHelper.getVAID();
        } catch (Throwable t) {

        }
        return null;
    }

    public static PackageInfo getPackageInfoWithUri(Context context, String path) {
        try {
            return context.getPackageManager().getPackageArchiveInfo(path, 0);

        } catch (Throwable e) {
            //TODO: handle exception
        }
        return null;
    }

    /**
     * Can be used by background threads and other objects without a context to attempt to get
     * ClientMetadata. If the object has never been referenced from a thread with a context,
     * this will return null.
     */
    public static ClientMetadata getInstance() {
        if (sInstance == null) {
            synchronized (ClientMetadata.class) {
                if (sInstance == null) sInstance = new ClientMetadata();
            }
        }
        return sInstance;
    }

    public static boolean isRoot() {
        try {
            return DeviceUtils.isRoot();
        } catch (Throwable ignored) {

        }
        return false;
    }

    public static boolean isEmulator() {
        try {
            return DeviceUtils.isEmulator();
        } catch (Throwable ignored) {

        }
        return false;
    }

    public static boolean isPermissionGranted(final Context context, final String permission) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context != null) {
                return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static void setOAIDCertPem(String certPem) {
        try {
            DevicesIDsHelper.setOAIDCertPem(certPem);
        } catch (Throwable th) {
            SigmobLog.e("not support OAID Module");
        }
    }

    public static void setOaidCertFileName(String fileName) {
        try {

            DevicesIDsHelper.setPemCustomFileName(fileName);
        } catch (Throwable th) {
            SigmobLog.e("not support OAID Module");
        }
    }

    public static Map<String, String> getQueryParamMap(final Uri uri) {

        final Map<String, String> params = new HashMap<>();
        for (final String queryParam : uri.getQueryParameterNames()) {
            params.put(queryParam, TextUtils.join(",", uri.getQueryParameters(queryParam)));
        }

        return params;
    }

    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public void setEnableLocation(boolean enableLocation) {
        this.enableLocation = enableLocation;
    }

    public int getScreenOrientation(Context context) {
        Display display = DeviceUtils.getDisplay(context);

        if (display == null) return Surface.ROTATION_0;

        return display.getRotation();
    }

    private String getOAIDSDK() {

        if (!TextUtils.isEmpty(mOAID_SDK)) {
            return mOAID_SDK;
        }

        try {
            DeviceHelper.getOAID(mContext, new DevicesIDsHelper.AppIdsUpdater() {
                @Override
                public void OnIdsAvalid(String oaid) {

                    if (!TextUtils.isEmpty(oaid)) {

                        mOAID_SDK = oaid;

                        if (!oaid.equalsIgnoreCase(mOaid)) {
                            SharedPreferences sharedPreferences = SharedPreferencesUtil.getSharedPreferences(mContext);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("o_aid_aes_gcm", AESUtil.EncryptString(oaid, Constants.AES_KEY));
                            editor.apply();
                        }

                    }
                    oaidSDKCallbacked = true;

                }
            });
        } catch (Throwable ignored) {

        }
        return mOAID_SDK;
    }

    public String getOAID_API() {
        try {

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && !Config.sharedInstance().getOaidApiDisable()) {

                if (!TextUtils.isEmpty(mOAID_API)) {
                    return mOAID_API;
                }
                DeviceHelper.getOAID_API(mContext, new DevicesIDsHelper.AppIdsUpdater() {
                    @Override
                    public void OnIdsAvalid(String oaid) {
                        if (!TextUtils.isEmpty(oaid)) {

                            if (oaid.equalsIgnoreCase(mOAID_API)) return;

                            mOAID_API = oaid;
                            if (!oaid.equalsIgnoreCase(mOaid)) {
                                SharedPreferences sharedPreferences = SharedPreferencesUtil.getSharedPreferences(mContext);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("o_aid_aes_gcm", AESUtil.EncryptString(oaid, Constants.AES_KEY));
                                editor.apply();
                            }

                        }
                    }
                });
            }

        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getAndroidId() {
        try {
            return DeviceUtils.getAndroidId(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getApkSha1() {
        try {
            return DeviceUtils.getApkSha1OrMd5(mContext, "SHA1");
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getApkMd5() {
        try {
            return DeviceUtils.getApkSha1OrMd5(mContext, "MD5");
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getAdvertisingId() {
        try {
            if (Constants.GOOGLE_PLAY) {
                return mIdentifierManager.getAdvertisingInfo().mAdvertisingId;
            }
        } catch (Throwable ignored) {

        }
        return null;
    }

    public boolean getLimitAdTrackingEnabled() {
        try {
            if (Constants.GOOGLE_PLAY) {
                return mIdentifierManager.getAdvertisingInfo().mDoNotTrack;
            }
        } catch (Throwable ignored) {

        }
        return false;
    }

    public String getDeviceId() {
        try {

            return getDeviceId(-1);
        } catch (Throwable t) {

            SigmobLog.e("getDeviceId:" + t.getMessage());
        }
        return null;
    }

    public synchronized String getDeviceId(int index) {
        try {
            if (TextUtils.isEmpty(mImei) && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

                if (!DeviceUtils.isCanUsePhoneState(mContext) || !DeviceUtils.isCanRetryIMEI())
                    return null;
                mImei = DeviceHelper.getIMEI(mContext);
                mImeiIndex0 = DeviceHelper.getIMEI(mContext, 0);
                mImeiIndex1 = DeviceHelper.getIMEI(mContext, 1);
            }

            if (index == -1) {
                return mImei;
            } else if (index == 0) {
                return mImeiIndex0;
            } else {
                return mImeiIndex1;
            }
        } catch (Throwable t) {
            SigmobLog.e("getDeviceId:" + t.getMessage());
        }
        return null;
    }

    public String getBootId() {
        try {
            if (Config.sharedInstance().isDisableBootMark()) return "";

            if (TextUtils.isEmpty(bootId)) {
                byte[] bootFromJNI = TanId.getBootFromJNI();

                if (bootFromJNI != null && bootFromJNI.length > 0) {
                    bootId = new String(bootFromJNI);
                }

                SigmobLog.i("origin bootId:" + bootId);

                if (!TextUtils.isEmpty(bootId)) {
                    bootId = bootId.replaceAll("\\s*|\t|\r|\n", "");
                    if (bootId.length() > 36) {
                        bootId = bootId.substring(0, 36);
                    }
                }

                SigmobLog.i("bootId:" + bootId);
            }
        } catch (Throwable t) {
            SigmobLog.i("getBootId:" + t.getMessage());
        }

        return bootId;
    }

    public String getUpdateId() {

        try {
            if (Config.sharedInstance().isDisableBootMark()) return "";

            if (TextUtils.isEmpty(updateId)) {
                updateId = TanId.getUpdateFromJNI();
                SigmobLog.i("updateId:" + updateId);
            }
        } catch (Throwable t) {
            SigmobLog.i("getUpdateId:" + t.getMessage());
        }
        return updateId;
    }

    public String getDeviceSerial() {
        try {
            return DeviceUtils.getDeviceSerial();
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getIMSI() {
        try {
            return DeviceUtils.getIMSI(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public static String getDeviceBrand() {
        return DeviceUtils.getDeviceBrand();
    }

    public boolean isTablet() {
        try {
            return DeviceUtils.isTablet(mContext);
        } catch (Throwable ignored) {

        }
        return false;
    }

    public Float getBatteryLevel() {
        try {
            return DeviceUtils.getBatteryLevel(mContext);
        } catch (Throwable ignored) {

        }
        return 0.0f;
    }

    public Integer getBatteryState() {
        try {
            return DeviceUtils.getBatteryState(mContext);
        } catch (Throwable ignored) {

        }
        return 0;
    }

    public Boolean getBatterySaveEnable() {
        try {
            return DeviceUtils.getBatterySaveEnable(mContext);
        } catch (Throwable ignored) {

        }
        return false;
    }

    public int getDensityDpi() {
        try {
            return (int) DeviceUtils.getDensityDpi(mContext);
        } catch (Throwable ignored) {

        }
        return 0;
    }

    public Integer getDeviceScreenHeightDip() {
        try {
            return (int) DeviceUtils.getDeviceScreenHeightDip(mContext);
        } catch (Throwable ignored) {

        }
        return 0;
    }

    public Integer getDeviceScreenWidthDip() {
        try {
            return (int) DeviceUtils.getDeviceScreenWidthDip(mContext);
        } catch (Throwable ignored) {

        }
        return 0;
    }

    public String getSDCardPath() {
        try {
            return DeviceUtils.getSDCardPath(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public Integer getDeviceScreenRealHeightDip() {
        try {
            return DeviceUtils.getDeviceScreenRealHeightDip(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public Integer getDeviceScreenRealWidthDip() {
        try {
            return DeviceUtils.getDeviceScreenRealWidthDip(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getTargetSdkVersion() {
        try {
            if (!TextUtils.isEmpty(targetSdkVersion)) return targetSdkVersion;
            targetSdkVersion = String.valueOf(mContext.getApplicationInfo().targetSdkVersion);
            return targetSdkVersion;
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getAppPackageName() {
        try {
            return AppPackageUtil.getAppPackageName(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public Integer getOrientationInt() {
        try {
            return DeviceUtils.getOrientationInt(mContext);
        } catch (Throwable ignored) {

        }
        return 0;
    }

    public String getAppVersion() {
        try {
            return AppPackageUtil.getAppVersionFromContext(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public int getActiveNetworkType() {
        try {
            return DeviceUtils.getActiveNetworkType().getId();
        } catch (Throwable ignored) {

        }
        return DeviceUtils.NetworkType.UNKNOWN.getId();
    }

    public String getNetworkOperatorForUrl() {
        try {
            return DeviceUtils.getNetworkOperatorForUrl(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getNetworkOperatorName() {
        try {
            return DeviceUtils.getNetworkOperatorName(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public int getSimOperatorName() {
        try {
            return DeviceUtils.getSimOperatorName(mContext);
        } catch (Throwable ignored) {

        }
        return 0;
    }

    public String getWifimac() {
        try {
            return DeviceUtils.getWifimac(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    private List<String> applist;

    public List<String> getAppList() {
        try {
            if (applist == null) {
                applist = new ArrayList<>();
            }

            if (!applist.isEmpty()) {
                return applist;
            }

            List<PackageInfo> appInfos = mContext.getPackageManager().getInstalledPackages(0);
            for (int i = 0; i < appInfos.size(); i++) {
                PackageInfo packageInfo = appInfos.get(i);
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    applist.add(packageInfo.packageName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return applist;
    }

    public String getWifiName() {
        try {
            return DeviceUtils.getWifiName(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getBlueToothName() {
        try {
            return DeviceUtils.getBlueToothName(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public Locale getDeviceLocale() {
        try {
            return DeviceUtils.getDeviceLocale(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public String getOAID() {
        int disable_up_oAid = Config.sharedInstance().getDisable_up_OAid();
        boolean oaidApiDisable = Config.sharedInstance().getOaidApiDisable();

        if ((disable_up_oAid < 0 || disable_up_oAid > 1) && oaidApiDisable) {
            return null;
        }

        String oaid = getOAID_SDK();

        if (TextUtils.isEmpty(oaid) && oaidSDKCallbacked) {
            oaid = getOAID_API();
        }

        if (!TextUtils.isEmpty(oaid) && !oaid.equalsIgnoreCase(mOaid)) {
            mOaid = oaid;
            return oaid;
        }

        return mOaid;

    }

    public String getOAID_SDK() {
        try {

            int disable_up_oAid = Config.sharedInstance().getDisable_up_OAid();

            switch (disable_up_oAid) {
                case 0:
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        return getOAIDSDK();
                    }
                    break;
                case 1:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//不禁止上传并且大于23
                        return getOAIDSDK();
                    }
                    break;
//                case 2://禁止上传
//                    return null;
            }
        } catch (Throwable ignored) {

        }
        return null;
    }

    public DisplayMetrics getRealMetrics() {
        try {
            return DeviceUtils.getRealMetrics(mContext);
        } catch (Throwable ignored) {

        }
        return DeviceUtils.getDisplayMetrics(mContext);
    }

    @Override
    public void onIdChanged(AdvertisingId oldId, AdvertisingId newId) {
    }

    public void setWindInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            if (insets != null && insets.isRound()) {
                mInsetBottom = insets.getSystemWindowInsetBottom();
            }
        }
    }

    public int getInsetBottom() {
        return mInsetBottom;
    }

    public boolean isRetryAble() {
        return mIsRetryAble;
    }

    public void setRetryAble(boolean retryAble) {
        mIsRetryAble = retryAble;
    }

    public String getStringResources(String name, String defaultString) {
        if (mContext != null) {
            Resources res = mContext.getResources();
            if (res != null) {
                int resId = res.getIdentifier(name, "string", mContext.getPackageName());

                if (resId != 0) {
                    return res.getString(resId);
                } else {
                    return defaultString;
                }
            }

        }
        return defaultString;
    }

    public int getStyleResources(String name) {
        int resId = 0;
        if (mContext != null) {
            Resources res = mContext.getResources();
            if (res != null) {
                resId = res.getIdentifier(name, "style", mContext.getPackageName());
            }
        }
        return resId;
    }

    public String getStringResources(String name, String defaultString, Object... formatArgs) {
        if (mContext != null) {
            Resources res = mContext.getResources();
            if (res != null) {
                int resId = res.getIdentifier(name, "string", mContext.getPackageName());
                if (resId != 0) {
                    SigmobLog.d("getStringResources resid" + resId);
                    return res.getString(resId, formatArgs);
                } else {
                    return String.format(defaultString, formatArgs);
                }
            }

        }
        return defaultString;
    }

    /**
     * Returns the singleton ClientMetadata object, using the context to obtain data if necessary.
     */
    public synchronized void initialize(Context context) {
        // Use a local variable so we can reduce accesses of the volatile field.
        if (mContext == null) {
            mContext = context.getApplicationContext();

            install_time = SharedPreferencesUtil.getSharedPreferences(mContext).getLong("install_time", 0);

            if (install_time == 0) {
                PackageInfo packageInfo = AppPackageUtil.getPackageInfo(context);

                if (packageInfo == null) {
                    install_time = System.currentTimeMillis() / 1000;
                } else {
                    install_time = packageInfo.firstInstallTime / 1000;
                }

                SharedPreferences.Editor edit = SharedPreferencesUtil.getSharedPreferences(mContext).edit();
                edit.putLong("install_time", install_time);
                edit.apply();
            }

            String o_aid_aes_gcm = SharedPreferencesUtil.getSharedPreferences(mContext).getString("o_aid_aes_gcm", null);
            if (o_aid_aes_gcm != null) {
                mOaid = AESUtil.DecryptString(o_aid_aes_gcm, Constants.AES_KEY);
            }

            mIdentifierManager = new IdentifierManager(mContext, this);
            SQLiteMTAHelper.initialize(mContext);
            SQLiteTrackHelper.initialize(mContext);
            BuriedPointManager.getInstance().start();
            DeviceUtils.registerNetworkChange(mContext);

            TrackManager.getInstance().startRetryTracking();
            ThreadPoolFactory.BackgroundThreadPool.getInstance().submit(new Runnable() {
                @Override
                public void run() {
                    RomUtils.getRomInfo();
                }
            });
        }
    }

    public Context getContext() {
        return mContext;
    }

    public String getDeviceName() {
        try {
            return DeviceUtils.getDeviceName(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public Long getSystemTotalMemorySize() {
        try {
            return DeviceUtils.getSysteTotalMemorySize(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public DisplayMetrics getDisplayMetrics() {
        try {
            return DeviceUtils.getDisplayMetrics(mContext);
        } catch (Throwable ignored) {

        }
        return null;
    }

    public boolean isNetworkConnected(String configUrl) {
        try {
            URL url = new URL(configUrl);
            return SigmobRequestUtil.isConnection(url.getHost());
        } catch (Throwable ignored) {
        }
        return false;
    }

    public String getRotation() {
        try {
            return DeviceUtils.getRotation(mContext);
        } catch (Throwable ignored) {
        }
        return null;
    }

    DownloadManager getDownloadManager() {
        return (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    private boolean isNoOptions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = null;
        intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);

        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return !list.isEmpty();
    }

    public LocationManager getLocationManager() {
        if (!Constants.GOOGLE_PLAY) {
            try {

                if (!DeviceUtils.isCanUseLocation(mContext)) return null;

                return (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

            } catch (Throwable throwable) {
                SigmobLog.e(throwable.getMessage());
            }
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public Location getLocation() {
        try {
            if (!enableLocation) {
                return null;
            }

            if (mLocation != null) {
                return mLocation;
            }

            LocationManager locationManager = getLocationManager();
            if (locationManager != null && DeviceUtils.isCanRetryLocation()) {

                SigmobLog.d("private :use_location ");
                Location l = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                if (l != null) {
                    // Found best last known location: %s", l);
                    mLocation = l;
                }
            }

        } catch (Exception e) {
            //TODO: handle exception
        }

        return mLocation;
    }

    public void setLocation(Location location) {

        mLocation = location;
    }

    public String getPermission(Context context) {
        String permissionReq = "";
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo pack = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            String[] permissionStrings = pack.requestedPermissions;
            for (int i = 0; i < permissionStrings.length; i++) {
                boolean permission = (PackageManager.PERMISSION_GRANTED == pm.checkPermission(permissionStrings[i], packageName));
                if (permission) {
                    if (i == permissionStrings.length - 1) {
                        permissionReq = permissionReq + permissionStrings[i];
                    } else {
                        permissionReq = permissionReq + permissionStrings[i] + ",";
                    }
                }
            }
            SigmobLog.d("permissionReq:" + permissionReq);
            if (!TextUtils.isEmpty(permissionReq)) {
                return Base64.encodeToString(permissionReq.getBytes(), Base64.NO_WRAP);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return permissionReq;
    }

    /**
     * SD卡判断
     *
     * @return
     */
    public boolean isSDCardAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * public String getAdvertisingId(){
     * return  mIdentifierManager.getAdvertisingInfo().mAdvertisingId;
     * }
     * /**
     *
     * @return the name of the application the SDK is included in.
     */
    public long getInstallTime() {
        return install_time;
    }

    public String getAppName() {
        return AppPackageUtil.getAppName(mContext);
    }

    public enum ForceOrientation {
        FORCE_PORTRAIT("portrait"), FORCE_LANDSCAPE("landscape"), DEVICE_ORIENTATION("device"), UNDEFINED("");

        private final String mKey;

        ForceOrientation(final String key) {
            mKey = key;
        }

    }
}