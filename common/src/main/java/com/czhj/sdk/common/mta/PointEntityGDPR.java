package com.czhj.sdk.common.mta;

public abstract class PointEntityGDPR extends PointEntitySuper {

    private String user_consent;
    private String age_restricted;
    private String age;
    private String gdpr_dialog_region;
    private String gdpr_region;
    private String is_unpersonalized;
    private String is_minor;

    public String getUser_consent() {
        return user_consent;
    }

    public void setUser_consent(String user_consent) {
        this.user_consent = user_consent;
    }

    public String getAge_restricted() {
        return age_restricted;
    }

    public void setAge_restricted(String age_restricted) {
        this.age_restricted = age_restricted;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGdpr_dialog_region() {
        return gdpr_dialog_region;
    }

    public void setGdpr_dialog_region(String gdpr_dialog_region) {
        this.gdpr_dialog_region = gdpr_dialog_region;
    }

    public String getGdpr_region() {
        return gdpr_region;
    }

    public void setGdpr_region(String gdpr_region) {
        this.gdpr_region = gdpr_region;
    }

    public void setIs_unpersonalized(String is_unpersonalized) {
        this.is_unpersonalized = is_unpersonalized;
    }

    public String getIs_unpersonalized() {
        return is_unpersonalized;
    }

    public void setIs_minor(String is_minor) {
        this.is_minor = is_minor;
    }

    public String getIs_minor() {
        return is_minor;
    }
}
