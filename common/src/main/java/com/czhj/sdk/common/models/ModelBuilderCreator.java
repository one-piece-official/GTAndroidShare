package com.czhj.sdk.common.models;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.security.NetworkSecurityPolicy;
import android.text.TextUtils;

import com.czhj.sdk.common.ClientMetadata;
import com.czhj.sdk.common.mta.DeviceContext;
import com.czhj.sdk.common.network.Networking;
import com.czhj.sdk.common.utils.RomUtils;
import com.czhj.sdk.logger.SigmobLog;

import java.io.File;
import java.lang.reflect.Field;
import java.util.TimeZone;


public class ModelBuilderCreator {

    public static Version.Builder createVersion(String version) {
        int major, micro, minor;

        try {
            String[] array = version.split("\\.");

            if (array.length > 2) {
                major = Integer.parseInt(array[0]);
                minor = Integer.parseInt(array[1]);
                micro = Integer.parseInt(array[2]);
            } else if (array.length > 1) {
                major = Integer.parseInt(array[0]);
                minor = Integer.parseInt(array[1]);
                micro = 0;
            } else if (array.length > 0) {
                major = Integer.parseInt(array[0]);
                micro = 0;
                minor = 0;
            } else {
                major = 0;
                micro = 0;
                minor = 0;
            }
        } catch (Throwable e) {
            major = 0;
            micro = 0;
            minor = 0;
        }

        Version.Builder version1 = createVersion(major, micro, minor);
        return version1.version_str(version);

    }

    private static Version.Builder createVersion(int major, int micro, int minor) {
        Version.Builder builder = new Version.Builder();

        builder.major(major);
        builder.micro(micro);
        builder.minor(minor);
        return builder;
    }


    public static Geo.Builder createGeo() {
        return createGeo(null);
    }

    public static Geo.Builder createGeo(DeviceContext deviceContext) {

        Geo.Builder builder = new Geo.Builder();

        try {
            //set Country
            builder.country(ClientMetadata.getInstance().getDeviceLocale().getCountry());

            //setLanguage
            builder.language(ClientMetadata.getInstance().getDeviceLocale().getLanguage().toUpperCase());

            //set location
            Location location = deviceContext != null ? deviceContext.getLocation() : ClientMetadata.getInstance().getLocation();
            if (location != null) {
                builder.lat((float) location.getLatitude());
                builder.lon((float) location.getLongitude());
                if (location.hasAccuracy()) {
                    builder.accuracy((double) location.getAccuracy());
                } else {
                    builder.accuracy(500.0);
                }
            }

            //set timeZone
            builder.timeZone(TimeZone.getDefault().getID());
            builder.secondsFromGMT(String.valueOf(TimeZone.getDefault().getRawOffset()));
        } catch (Throwable e) {
            SigmobLog.e("Geo Builder failed", e);

        }

        return builder;
    }


    public static DeviceId.Builder createDeviceId(DeviceContext deviceContext) {

        DeviceId.Builder builder = new DeviceId.Builder();

        try {
            String androidId = deviceContext != null ? deviceContext.getAndroidId() : ClientMetadata.getInstance().getAndroidId();
            if (!TextUtils.isEmpty(androidId)) {
                builder.android_id(androidId);
            }

            String gaid = ClientMetadata.getInstance().getAdvertisingId();
            if (!TextUtils.isEmpty(gaid)) {
                builder.gaid(gaid);
            }

            String uid = ClientMetadata.getInstance().getUid();
            if (!TextUtils.isEmpty(uid)) {
                builder.uid(uid);
            }


            String imei = deviceContext != null ? deviceContext.getImei() : ClientMetadata.getInstance().getDeviceId();
            if (!TextUtils.isEmpty(imei)) {
                builder.imei(imei);
            }
            String buildSerial = ClientMetadata.getInstance().getDeviceSerial();
            if (!TextUtils.isEmpty(buildSerial)) {
                builder.android_uuid(buildSerial);
            }
            String imsi = ClientMetadata.getInstance().getIMSI();
            if (!TextUtils.isEmpty(imsi)) {
                builder.imsi(imsi);
            }
            String imei1 = deviceContext != null ? deviceContext.getImei1() : ClientMetadata.getInstance().getDeviceId(0);
            if (!TextUtils.isEmpty(imei1)) {
                builder.imei1(imei1);
            }

            String imei2 = deviceContext != null ? deviceContext.getImei2() : ClientMetadata.getInstance().getDeviceId(1);
            if (!TextUtils.isEmpty(imei2)) {
                builder.imei2(imei2);
            }


            String brand = ClientMetadata.getInstance().getDeviceBrand();
            if (!TextUtils.isEmpty(brand)) {
                builder.brand(brand);
            }


            try {
                String oaid = deviceContext != null ? deviceContext.getOaid() : ClientMetadata.getInstance().getOAID();
                if (!TextUtils.isEmpty(oaid)) {
                    builder.oaid(oaid);
                }
            } catch (Throwable throwable) {
                SigmobLog.e("getOAID " + throwable.getMessage());
            }

//            try {
//                String oaid_api = ClientMetadata.getInstance().getOAID_API();
//                if (!TextUtils.isEmpty(oaid_api)) {
//                    builder.oaid_api(oaid_api);
//                }
//            } catch (Throwable throwable) {
//                SigmobLog.e("getOAID_API " + throwable.getMessage());
//            }

            try {
                String vaid = ClientMetadata.getInstance().getVAID();
                if (!TextUtils.isEmpty(vaid)) {
                    builder.vaid(vaid);
                }
            } catch (Throwable throwable) {
                SigmobLog.e("getVAID " + throwable.getMessage());
            }

            try {

                if (deviceContext != null) {
                    builder.is_custom_imei(!deviceContext.isCustomPhoneState());
                    builder.is_custom_oaid(deviceContext.isCustomOaId());
                    builder.is_custom_android_id(!deviceContext.isCustomAndroidId());
                }

            } catch (Throwable throwable) {
                SigmobLog.e("getCustomController " + throwable.getMessage());
            }

        } catch (Throwable throwable) {
            SigmobLog.e("DeviceId Builder failed", throwable);

        }

        return builder;
    }

    public static DeviceId.Builder createDeviceId() {
        return createDeviceId(null);
    }

    public static Device.Builder createDevice() {
        return createDevice(null);
    }

    public static Device.Builder createDevice(DeviceContext deviceContext) {

        Device.Builder builder = new Device.Builder();

        boolean istable = ClientMetadata.getInstance().isTablet();

        builder.device_type(istable ? 5 : 4);

        builder.os_type(2);

        try {
            Size.Builder resolutionBuilder = new Size.Builder();
            resolutionBuilder.height = ClientMetadata.getInstance().getRealMetrics().heightPixels;
            resolutionBuilder.width = ClientMetadata.getInstance().getRealMetrics().widthPixels;

            builder.resolution(resolutionBuilder.build());

            builder.battery_level(ClientMetadata.getInstance().getBatteryLevel());

            builder.battery_state(ClientMetadata.getInstance().getBatteryState());
            builder.battery_save_enabled(ClientMetadata.getInstance().getBatterySaveEnable());

            builder.dpi(ClientMetadata.getInstance().getDensityDpi());

            builder.os_version(createVersion(ClientMetadata.getDeviceOsVersion()).build());
            builder.vendor(ClientMetadata.getDeviceManufacturer());

            builder.is_root(ClientMetadata.isRoot());
            Size.Builder sizeBuilder = new Size.Builder().height(ClientMetadata.getInstance().getDeviceScreenHeightDip()).width(ClientMetadata.getInstance().getDeviceScreenWidthDip());

            String model = ClientMetadata.getDeviceModel();
            if (!TextUtils.isEmpty(model)) {
                builder.model(ClientMetadata.getDeviceModel());
            }

            builder.screen_size(sizeBuilder.build());
            builder.geo(createGeo(deviceContext).build());
            builder.disk_size(Environment.getDataDirectory().getTotalSpace());

            String name = ClientMetadata.getInstance().getDeviceName();
            if (!TextUtils.isEmpty(name)) builder.device_name(name);

            builder.start_timestamp(ClientMetadata.getBootSystemTime());

            builder.android_api_level(ClientMetadata.getDeviceOSLevel());

            builder.mem_size(ClientMetadata.getInstance().getSystemTotalMemorySize());

            builder.total_disk_size(Environment.getDataDirectory().getTotalSpace());
            builder.free_disk_size(Environment.getDataDirectory().getFreeSpace());

            String sdPath = ClientMetadata.getInstance().getSDCardPath();
            if (!TextUtils.isEmpty(sdPath)) {
                builder.sd_total_disk_size(new File(sdPath).getTotalSpace());
                builder.sd_free_disk_size(new File(sdPath).getFreeSpace());
            }

            String bootId = ClientMetadata.getInstance().getBootId();

            if (!TextUtils.isEmpty(bootId)) {
                builder.boot_mark(bootId);
            }

            String updateId = ClientMetadata.getInstance().getUpdateId();

            if (!TextUtils.isEmpty(updateId)) {
                builder.update_mark(updateId);
            }

            RomUtils.RomInfo romInfo = RomUtils.getRomInfo();
            if (romInfo != null) {
                builder.rom_name(romInfo.getName());
                Version.Builder version = createVersion(romInfo.getVersion());
                version.version_str(romInfo.getVersion());
                builder.rom_version(version.build());
            }


        } catch (Throwable e) {

            SigmobLog.e("Device Builder failed", e);
        }

        return builder;

    }

    public static WXProgramReq.Builder createWXProgramReq() {

        WXProgramReq.Builder builder = new WXProgramReq.Builder();

        try {
//            IWXAPI api = WXAPIFactory.createWXAPI(this, appId, false);
//            boolean wxAppInstalled = api.isWXAppInstalled();
//            int wxAppSupportAPI = api.getWXAppSupportAPI();
//
//            int majorVersion = Build.getMajorVersion();
//            int minorVersion = Build.getMinorVersion();
//            int sdkInt = Build.SDK_INT;
//            String SDK_VERSION_NAME = Build.SDK_VERSION_NAME;

            Class factory = Class.forName("com.tencent.mm.opensdk.openapi.WXAPIFactory");
            java.lang.reflect.Method createWXAPI = factory.getMethod("createWXAPI", Context.class, String.class);
            createWXAPI.setAccessible(true);
            Object api = createWXAPI.invoke(factory, ClientMetadata.getInstance().getContext(), "");

            java.lang.reflect.Method isWXAppInstalled = api.getClass().getMethod("isWXAppInstalled");
            isWXAppInstalled.setAccessible(true);
            boolean appInstalled = (boolean) isWXAppInstalled.invoke(api);

            java.lang.reflect.Method getWXAppSupportAPI = api.getClass().getMethod("getWXAppSupportAPI");
            getWXAppSupportAPI.setAccessible(true);
            int appSupportAPI = (int) getWXAppSupportAPI.invoke(api);

            Class build = Class.forName("com.tencent.mm.opensdk.constants.Build");

//            java.lang.reflect.Method getMajorVersion = build.getMethod("getMajorVersion");
//            getMajorVersion.setAccessible(true);
//            int majorVersion = (int) getMajorVersion.invoke(build);

            Field f = build.getDeclaredField("SDK_INT");
            f.setAccessible(true);
            int SDK_INT = (int) f.get(null);

            builder.wx_installed(appInstalled);
            builder.wx_api_ver(appSupportAPI);
            builder.opensdk_ver(String.valueOf(SDK_INT));
        } catch (Throwable throwable) {
            SigmobLog.e("createWXProgramReq failed" + throwable.getMessage());
        }

        return builder;
    }


    public static App.Builder createApp() {
        App.Builder builder = new App.Builder();
        //TODO: createrAppPackage

        try {

            if (ClientMetadata.getInstance().getAppPackageName() != null) {
                builder.app_package(ClientMetadata.getInstance().getAppPackageName());
            }

            builder.orientation(ClientMetadata.getInstance().getOrientationInt());

            String appName = ClientMetadata.getInstance().getAppName();
            if (!TextUtils.isEmpty(appName)) {
                builder.name(appName);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.support_http = NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted();
            } else {
                builder.support_http = true;
            }
            String appVer = ClientMetadata.getInstance().getAppVersion();
            if (!TextUtils.isEmpty(appVer)) {
                Version.Builder versBuilder = createVersion(appVer);
                builder.app_version(versBuilder.build());
            }


            builder.idfv("android");

            builder.sdk_ext_cap.add(3);


            long installTime = ClientMetadata.getInstance().getInstallTime();

            builder.install_time(installTime);
        } catch (Throwable e) {
            SigmobLog.e("App Builder failed", e);
        }


        return builder;
    }

    public static AdSlot.Builder createAdSlot() {

        return new AdSlot.Builder();
    }

    public static Network.Builder createNetwork() {

        return createNetwork(null);
    }

    public static Network.Builder createNetwork(DeviceContext deviceContext) {

        Network.Builder builder = new Network.Builder();
        try {
            builder.connection_type(ClientMetadata.getInstance().getActiveNetworkType());


            String operator = ClientMetadata.getInstance().getNetworkOperatorForUrl();

            if (!TextUtils.isEmpty(operator)) {
                builder.operator(operator);
            }

            String ua = Networking.getUserAgent();

            if (!TextUtils.isEmpty(ua)) {
                builder.ua(ua);
            }


            builder.connection_type(ClientMetadata.getInstance().getActiveNetworkType());


            String macAddr = ClientMetadata.getInstance().getMacAddress();
            if (!TextUtils.isEmpty(macAddr)) builder.mac(macAddr);

            String wifiMac = ClientMetadata.getInstance().getWifimac();

            if (!TextUtils.isEmpty(wifiMac)) builder.wifi_mac(wifiMac);


            String wifiName = ClientMetadata.getInstance().getWifiName();
            if (!TextUtils.isEmpty(wifiName)) {
                builder.wifi_id(wifiName);
            }

            String operatorName = deviceContext != null ? deviceContext.getCarrierName() : ClientMetadata.getInstance().getNetworkOperatorName();
            if (!TextUtils.isEmpty(operatorName)) {
                builder.carrier_name(operatorName);
            }

        } catch (Throwable e) {

            SigmobLog.e("Network Builder failed", e);

        }
        return builder;
    }
}
