package com.czhj.sdk.common;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;

public class Constants {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final boolean GOOGLE_PLAY = false;

    // Internal Video Tracking nouns, defined in ad server

    public static final String TOKEN = "token";

    public static final int SDK_VERSION = 171;
    public static final int SIG_VERSION = 159;

    public static final String SUCCESS = "1";
    public static final String FAIL = "0";
    public static final int TEN_SECONDS_MILLIS = 10 * 1000;
    public static final String BROADCAST_IDENTIFIER_KEY = "broadcastIdentifier";

    //adapter
    public static final String TEMPLATETYPE = "templateType";
    public static final String SHOW_DOWNLOAD_DIALOG = "showDownloadDialog";//0不显示；1显示下载弹框
    public static final String CLICK_TYPE = "clickType";//0按钮点击；1全屏点击

    public static final String ADSCENE = "ad_scene";

    //gdpr
    public static final String IS_REQUEST_IN_EEA_OR_UNKNOWN = "is_request_in_eea_or_unknown";
    public final static String EXT_GDPR_REGION = "gdpr_region";
    public static final String GDPR_CONSENT_STATUS = "consent_status";
    //coppa
    public static final String USER_AGE = "user_age";
    public static final String AGE_RESTRICTED_STATUS = "age_restricted_status";

    public static final String AD_SCENE_ID = "scene_id";
    public static final String AD_SCENE_DESC = "scene_desc";
    public static final String LOAD_ID = "loadId";

    public static final boolean ENCRYPT = false;

    public static final String SDK_COMMON_FOLDER = "gts";
    public static final String AES_KEY = "gt_android_mta_db";
    public static final String GCM_NONCE = "+lx3fUZcRI2mzU/W";
    public static final int RETRY_MAX_NUM = 3000;
    public static final String LOG_URL = "dc.sigmob.cn/log";

    @SuppressLint("SimpleDateFormat")
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static int getVersion() {
        return SDK_VERSION;
    }
}
