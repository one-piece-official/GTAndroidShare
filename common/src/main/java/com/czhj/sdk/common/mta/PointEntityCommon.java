package com.czhj.sdk.common.mta;


import android.location.Location;
import android.text.TextUtils;
import android.util.Base64;

import com.czhj.sdk.common.ClientMetadata;
import com.czhj.sdk.common.Constants;
import com.czhj.sdk.common.network.Networking;


public abstract class PointEntityCommon extends PointEntitySuper {

    public String getTargetSdkVersion() {
        return ClientMetadata.getInstance().getTargetSdkVersion();
    }

    public String getDevice_model() {
        return ClientMetadata.getDeviceModel();
    }

    public String getDevice_type() {
        return ClientMetadata.getInstance().isTablet() ? "5" : "4";
    }

    public String getPackageName() {
        return ClientMetadata.getInstance().getAppPackageName();
    }

    public String getIsEmulator() {
        return ClientMetadata.isEmulator() ? Constants.SUCCESS : Constants.FAIL;
    }

    public String getScreenDensity() {
        return String.valueOf(ClientMetadata.getInstance().getDensityDpi());
    }

    public String getWifi_id() {
        String wifi_id = ClientMetadata.getInstance().getWifiName();
        if (TextUtils.isEmpty(wifi_id)) {

            return wifi_id;
        }
        return Base64.encodeToString(ClientMetadata.getInstance().getWifiName().getBytes(), Base64.NO_WRAP);

    }

    public String getWifi_mac() {
        return ClientMetadata.getInstance().getWifimac();
    }

    public String getLat() {
        DeviceContext deviceContext = getDeviceContext();
        Location location = deviceContext != null ? deviceContext.getLocation() : ClientMetadata.getInstance().getLocation();

        if (location != null) {
            return String.valueOf(location.getLatitude());
        }
        return null;
    }

    public String getLng() {
        DeviceContext deviceContext = getDeviceContext();
        Location location = deviceContext != null ? deviceContext.getLocation() : ClientMetadata.getInstance().getLocation();

        if (location != null) {
            return String.valueOf(location.getLongitude());
        }
        ;
        return null;
    }

    public String getBattery_level() {
        return String.format("%.2f", ClientMetadata.getInstance().getBatteryLevel());
    }

    public String getBattery_save_enabled() {
        return String.valueOf(ClientMetadata.getInstance().getBatterySaveEnable());
    }

    public String getBattery_state() {
        return String.valueOf(ClientMetadata.getInstance().getBatteryState());
    }

    public String getC_width() {
        return String.valueOf(ClientMetadata.getInstance().getDeviceScreenWidthDip());
    }

    public String getC_height() {
        return String.valueOf(ClientMetadata.getInstance().getDeviceScreenHeightDip());
    }

    public String getD_width() {
        return String.valueOf(ClientMetadata.getInstance().getDeviceScreenRealWidthDip());
    }

    public String getD_height() {
        return String.valueOf(ClientMetadata.getInstance().getDeviceScreenRealHeightDip());
    }

    public String getScreen_angle() {
        return String.valueOf(Math.abs(ClientMetadata.getInstance().getOrientationInt() - 1) * 90);
    }

    public String getClient_pixel() {
        return String.format("%sx%s", ClientMetadata.getInstance().getDisplayMetrics().widthPixels, ClientMetadata.getInstance().getDisplayMetrics().heightPixels);
    }

    public String getResolution() {
        return String.format("%sx%s", ClientMetadata.getInstance().getRealMetrics().widthPixels, ClientMetadata.getInstance().getRealMetrics().heightPixels);
    }

    public String getBrowser() {
        return Networking.getUserAgent();
    }

    public String getBrand() {
        return ClientMetadata.getDeviceBrand();
    }

    public String getVender() {
        return ClientMetadata.getDeviceManufacturer();
    }

}
