package com.czhj.sdk.common.mta;


public abstract class PointEntityBase extends PointEntitySuper {

    private String adType;
    private String placement_id;
    private String load_id;
    private String platform;

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public String getPlacement_id() {
        return placement_id;
    }

    public void setPlacement_id(String placement_id) {
        this.placement_id = placement_id;
    }

    public String getLoad_id() {
        return load_id;
    }

    public void setLoad_id(String load_id) {
        this.load_id = load_id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
