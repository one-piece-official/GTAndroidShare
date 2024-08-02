package com.czhj.sdk.common;

import android.location.Location;

/**
 * created by lance on   2022/11/23 : 3:32 下午
 */
public abstract class CustomController {

    public CustomController() {

    }

    public boolean isCanUseLocation() {
        return true;
    }

    public Location getLocation() {
        return null;
    }

    public boolean isCanUsePhoneState() {
        return true;
    }

    public String getDevImei() {
        return null;
    }

    public boolean isCanUseAndroidId() {
        return true;
    }

    public String getAndroidId() {
        return null;
    }

    public String getDevOaid() {
        return null;
    }

}
