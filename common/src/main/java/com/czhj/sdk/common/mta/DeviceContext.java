package com.czhj.sdk.common.mta;

import android.location.Location;

public abstract class DeviceContext {

    public String getAndroidId() {
        return null;
    }

    public String getImei() {
        return null;
    }

    public String getImei1() {
        return null;
    }

    public boolean isCustomPhoneState() {
        return false;
    }

    public boolean isCustomAndroidId() {
        return false;
    }

    public boolean isCustomOaId() {
        return false;
    }

    public String getImei2() {
        return null;
    }

    public String getOaid() {
        return null;
    }

    public Location getLocation() {
        return null;
    }

    public String getCarrier() {
        return null;
    }

    public String getCarrierName() {
        return null;
    }
}
