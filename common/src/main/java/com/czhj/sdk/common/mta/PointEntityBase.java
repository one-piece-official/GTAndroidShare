package com.czhj.sdk.common.mta;


public abstract class PointEntityBase extends PointEntitySuper {

    private String ad_type;
    private String load_id;

    public String getAd_type() {
        return ad_type;
    }

    public void setAd_type(String ad_type) {
        this.ad_type = ad_type;
    }

    public String getLoad_id() {
        return load_id;
    }

    public void setLoad_id(String load_id) {
        this.load_id = load_id;
    }
}
