package com.czhj.sdk.common.mta;


public abstract class PointEntityBase extends PointEntitySuper {

    private String adtype;
    private String placement_id;
    private String load_id;
    private String platform;
    private String vtime;
    private String ad_scene;
    private String scene_desc;
    private String scene_id;


    public String getAdtype() {
        return adtype;
    }

    public void setAdtype(String adtype) {
        this.adtype = adtype;
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

    public String getVtime() {
        return vtime;
    }

    public void setVtime(String vtime) {
        this.vtime = vtime;
    }

    public String getAd_scene() {
        return ad_scene;
    }

    public void setAd_scene(String ad_scene) {
        this.ad_scene = ad_scene;
    }

    public String getScene_desc() {
        return scene_desc;
    }

    public void setScene_desc(String scene_desc) {
        this.scene_desc = scene_desc;
    }

    public String getScene_id() {
        return scene_id;
    }

    public void setScene_id(String scene_id) {
        this.scene_id = scene_id;
    }
}
