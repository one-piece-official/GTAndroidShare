package com.czhj.sdk.common.mta;


public abstract class PointEntityBase extends PointEntitySuper {

    private String adType;
    private String load_id;

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public String getLoad_id() {
        return load_id;
    }

    public void setLoad_id(String load_id) {
        this.load_id = load_id;
    }
}
