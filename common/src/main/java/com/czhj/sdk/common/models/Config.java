package com.czhj.sdk.common.models;

import android.text.TextUtils;

import com.czhj.sdk.common.Constants;
import com.czhj.volley.DefaultRetryPolicy;
import com.czhj.volley.toolbox.Volley;

public class Config {

    private static Config gInstance;
    private int max_send_log_records = 100;
    private int send_log_interval = 3;
    private int networkTimeout = DefaultRetryPolicy.DEFAULT_CONNECT_TIMEOUT_MS;

    private Config() {

    }

    public static Config sharedInstance() {
        if (gInstance == null) {

            synchronized (Config.class) {
                if (gInstance == null) {
                    gInstance = new Config();
                }
            }
        }
        return gInstance;
    }

    private boolean mIsGDPRRegion = false;

    public boolean isGDPRRegion() {

        return mIsGDPRRegion;
    }

    private boolean disable_boot_mark;

    public boolean isDisableBootMark() {

        return disable_boot_mark;
    }

    private boolean disable_oaid_api;

    public boolean getOaidApiDisable() {

        return disable_oaid_api;
    }

    private int disable_up_oaid;

    public int getDisable_up_OAid() {
        return disable_up_oaid;
    }

    private String log;
    private boolean logenc;

    public void update(boolean isGDPRRegion, boolean disable_boot_mark, boolean disable_oaid_api,
                       int disable_up_oaid, String log, int send_log_interval, int max_send_log_records,
                       boolean logenc) {
        this.mIsGDPRRegion = isGDPRRegion;
        this.disable_boot_mark = disable_boot_mark;
        this.disable_oaid_api = disable_oaid_api;
        this.disable_up_oaid = disable_up_oaid;
        this.max_send_log_records = max_send_log_records;
        this.send_log_interval = send_log_interval;
        this.log = log;
        this.logenc = logenc;
    }

    public void setEnable_okhttp3(boolean enable_okhttp3) {
        Volley.setEnableOkhttp3(enable_okhttp3);
    }

    public int getMax_send_log_records() {

        if (max_send_log_records < 10) {
            max_send_log_records = 100;
        }
        return max_send_log_records;
    }

    public int getSend_log_interval() {
        return send_log_interval;
    }

    public String getLogUrl() {
        if (TextUtils.isEmpty(log)) {
            return new StringBuilder(Constants.HTTPS).append("://").append(Constants.LOG_URL).toString();
        }
        return log;
    }

    public boolean isEnc() {
        return logenc;
    }

    public void setNetworkTimeout(int networkTimeout) {
        if (networkTimeout > 0 && networkTimeout <= 30) {
            this.networkTimeout = networkTimeout*1000;
        }
    }

    public int getNetworkTimeout() {
        return networkTimeout;
    }
}
