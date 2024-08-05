package com.czhj.sdk.common.mta;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.czhj.sdk.common.ClientMetadata;
import com.czhj.sdk.common.Constants;
import com.czhj.sdk.common.Database.SQLiteBuider;
import com.czhj.sdk.common.Database.SQLiteMTAHelper;
import com.czhj.sdk.common.ThreadPool.ThreadPoolFactory;
import com.czhj.sdk.common.network.BuriedPointRequest;
import com.czhj.sdk.common.utils.AESUtil;
import com.czhj.sdk.common.utils.ReflectionUtil;
import com.czhj.sdk.logger.SigmobLog;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public abstract class PointEntitySuper {

    private final static String mSessionId = UUID.randomUUID().toString();
    private static long seqId = 1;
    protected Object mPointEntityClass;
    private String ac_type;
    private String category;
    private String sub_category;
    private String ext;
    private Map<String, String> options;
    private String timestamp;
    private String sha1;
    private String md5;
    private String name;
    private int integration;
    private String version;
    private int compatible;

    public PointEntitySuper() {

    }

    public abstract DeviceContext getDeviceContext();

    public String getTime_zone() {
        return TimeZone.getDefault().getID();
    }

    public String getSeconds_from_GMT() {
        return String.valueOf(TimeZone.getDefault().getRawOffset() / 1000 / 60 / 60);
    }

    public String getOs() {
        return "2";
    }

    public String getAppVersion() {
        return ClientMetadata.getInstance().getAppVersion();
    }

    private static synchronized long getSeqId() {
        return seqId++;
    }

    public static String lowFirstChar(String name) {
        char[] cs = name.toCharArray();

        if (cs[0] > 64 && cs[0] < 91) {
            cs[0] += 32;
            return String.valueOf(cs);
        }

        return name;
    }

    public static String captureName(String name) {
        char[] cs = name.toCharArray();
        if (cs[0] > 96 && cs[0] < 123) {
            cs[0] -= 32;
            return String.valueOf(cs);
        }
        return name;
    }

    /**
     * url encode
     *
     * @param sources 目标字符（UTF-8）
     */
    public static String toURLEncoded(String sources) {
        if (sources == null) return "";
        try {
            String str = new String(sources.getBytes(), "UTF-8");
            str = URLEncoder.encode(str, "UTF-8");
            return str;
        } catch (UnsupportedEncodingException e) {
            SigmobLog.e(e.getMessage());
        }
        return "";
    }

    public int getCompatible() {
        return compatible;
    }

    public void setCompatible(int compatible) {
        this.compatible = compatible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIntegration() {
        return integration;
    }

    public void setIntegration(int integration) {
        this.integration = integration;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getUdid() {
        return ClientMetadata.getInstance().getUDID();
    }

    public void commit() {
        mPointEntityClass = this;
        if (TextUtils.isEmpty(this.timestamp)) {
            setTimestamp(String.valueOf(System.currentTimeMillis()));
        }
        ThreadPoolFactory.BackgroundThreadPool.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                insertToDB(null);
            }
        });
    }

    public Map<String, String> getOptions() {
        if (options == null) {
            options = new HashMap<>();
        }
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public abstract boolean isAcTypeBlock();

    public abstract String appId();

    public void insertToDB(final SQLiteMTAHelper.ExecCallBack execCallBack) {
        //begin
        try {
            if (isAcTypeBlock() || TextUtils.isEmpty(appId())) {
                return;
            }

            Map<String, Object> map = toMap();

            if (getOptions() != null) {
                map.remove("options");
                map.putAll(getOptions());
            }

            String uniqueKey = "gt_android_" + appId();

            map.put("_unique_key", uniqueKey);

            String item = toJsonString(map);
            if (TextUtils.isEmpty(item)) {
                return;
            }

            SigmobLog.d("dc_debug:" + item);

            SQLiteMTAHelper mtaHelper = SQLiteMTAHelper.getInstance();

            if (mtaHelper == null) {
//                BuriedPointManager.getInstance().addWaitSend(item);
                return;
            }

            SQLiteDatabase database = mtaHelper.getWritableDatabase();

            SQLiteBuider.Insert.Builder builder = new SQLiteBuider.Insert.Builder();
            builder.setTableName(SQLiteMTAHelper.TABLE_POINT);
            Map<String, Object> values = new HashMap<>();
            values.put("item", AESUtil.EncryptString(item, Constants.AES_KEY));
            values.put("encryption", 1);
            builder.setColumnValues(values);

            SQLiteMTAHelper.insert(database, builder.build(), new SQLiteMTAHelper.ExecCallBack() {
                @Override
                public void onSuccess() {
                    SigmobLog.d("insert success!");
                    if (execCallBack != null) {
                        execCallBack.onSuccess();
                    }
                }

                @Override
                public void onFailed(Throwable e) {
                    if (execCallBack != null) {
                        execCallBack.onFailed(e);
                    }
                    SigmobLog.e(e.getMessage());
                }
            });
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        //end
    }

    private boolean urlEncodeFilter(String key) {
//        String[] filter = {"motion_before", "motion_after", "custom_info"};
        String[] filter = {"motion_before", "motion_after"};
        return Arrays.asList(filter).contains(key);
    }

    public String toJsonString(Map<String, Object> map) {

        if (map.isEmpty()) return null;

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        Iterator<Map.Entry<String, Object>> entryIterator = map.entrySet().iterator();

        boolean isValid = false;

        while (entryIterator.hasNext()) {

            Map.Entry entry = entryIterator.next();

            if (entry.getValue() != null) {
                if (isValid) {
                    builder.append(",");
                }
                builder.append("\"" + entry.getKey() + "\"" + ":");
                String value = null;
                if (entry.getValue() instanceof String) {
                    if (!urlEncodeFilter((String) entry.getKey()))
                        value = toURLEncoded((String) entry.getValue());
                    else {
                        value = (String) entry.getValue();
                    }
                } else {
                    value = entry.getValue().toString();
                }
                if (!value.startsWith("{")) builder.append("\"" + value + "\"");
                else {
                    builder.append(value);
                }
                isValid = true;
            }
        }
        builder.append("}");

        return builder.toString();
    }

    public void sendServe() {
        sendServe(null);
    }

    public void sendServe(BuriedPointRequest.RequestListener listener) {
        mPointEntityClass = this;

        Map<String, Object> map = toMap();

        String item = toJsonString(map);

        if (!TextUtils.isEmpty(item)) {

            try {
                StringBuilder jsonStringBuilder = new StringBuilder();
                jsonStringBuilder.append("[");
                jsonStringBuilder.append(item);
                jsonStringBuilder.append("]");

                String dcLog = jsonStringBuilder.toString();
                String rawData = BuriedPointManager.deflateAndBase64(dcLog);
                String body = toURLEncoded(rawData);
                BuriedPointRequest.BuriedPointSend(body, listener);
            } catch (Exception e) {
                SigmobLog.e(e.getMessage());
            }
        }
    }

    public Map<String, Object> toMap() {
        List<Method> methods = ReflectionUtil.getMethodWithTraversal(mPointEntityClass.getClass());

        HashMap<String, Object> map = new HashMap<>(methods.size());

        for (Method method : methods) {

            if (method.getName().startsWith("get") && (!method.getName().equals("getLogs") && !method.getName().equals("getDeviceContext"))) {
                try {
                    Object value = method.invoke(mPointEntityClass);
                    String key = lowFirstChar(method.getName().substring(3));

                    if (value != null) {
                        if (value instanceof String) {
                            String valueStr = (String) value;
                            if (TextUtils.isEmpty(valueStr)) {
                                continue;
                            }
                        }

                        if (key.equalsIgnoreCase("content_length")) {
                            map.put("content-length", value);
                        } else if (key.equalsIgnoreCase("content_type")) {
                            map.put("content-type", value);
                        } else if (key.equalsIgnoreCase("ac_type")) {
                            map.put("_ac_type", value);
                        } else if (key.equalsIgnoreCase("user_id")) {
                            map.put("_user_id", value);
                        } else if (!key.equalsIgnoreCase("class")) {
                            map.put(key, value);
                        }
                    }

                } catch (Throwable throwable) {
                    SigmobLog.e("name " + method.getName() + throwable.getMessage());
                }
            }
        }

        return map;
    }


    public String getUser_id() {
        return ClientMetadata.getUserId();
    }

    public String getAc_type() {
        return ac_type;
    }

    public void setAc_type(String ac_type) {
        this.ac_type = ac_type;
    }

    public String getSession_id() {
        return mSessionId;
    }

    public String getSeq_id() {
        return String.valueOf(PointEntitySuper.getSeqId());
    }

    public String getUid() {
        return ClientMetadata.getInstance().getUid();
    }

    public String getClientOsVersion() {
        return ClientMetadata.getDeviceOsVersion();
    }

    public String getImei2() {
        if (getDeviceContext() != null) {
            return getDeviceContext().getImei2();
        }
        return ClientMetadata.getInstance().getDeviceId(1);
    }

    public String getAndroid_id() {
        if (getDeviceContext() != null) {
            return getDeviceContext().getAndroidId();
        }
        return ClientMetadata.getInstance().getAndroidId();
    }

    public String getImei() {
        if (getDeviceContext() != null) {
            return getDeviceContext().getImei();
        }
        return ClientMetadata.getInstance().getDeviceId();
    }

    public String getImei1() {
        if (getDeviceContext() != null) {
            return getDeviceContext().getImei1();
        }
        return ClientMetadata.getInstance().getDeviceId(0);
    }

    public String getCarrier() {
        if (getDeviceContext() != null) {
            return getDeviceContext().getCarrier();
        }
        return String.valueOf(ClientMetadata.getInstance().getNetworkOperatorForUrl());
    }

    public String getGoogle_aid() {
        return ClientMetadata.getInstance().getAdvertisingId();
    }

    public String getOaid() {
        if (getDeviceContext() != null) {
            return getDeviceContext().getOaid();
        }
        return ClientMetadata.getInstance().getOAID();
    }

    public abstract String getSdkVersion();

    public String getNetworkType() {
        return String.valueOf(ClientMetadata.getInstance().getActiveNetworkType());
    }

    public String getTimestamp() {
        if (TextUtils.isEmpty(this.timestamp)) {
            return String.valueOf(System.currentTimeMillis());
        }
        return this.timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSub_category() {
        return sub_category;
    }

    public void setSub_category(String sub_category) {
        this.sub_category = sub_category;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

}
