package com.czhj.sdk.common.track;


import android.text.TextUtils;

import com.czhj.sdk.common.ClientMetadata;
import com.czhj.sdk.logger.SigmobLog;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseMacroCommon implements Serializable {

    private static final long serialVersionUID = 1L;
    private Map<String, String> mMacroMap = new HashMap<>();

    private Map<String, String> mServerMacroMap = new HashMap<>();

    public enum MacroCode {

        _MC_, _COUNTRY_, _TIMESTAMP_, _OSVERSION_, _BUNDLEID_, _LANGUAGE_, _TIMEMILLIS_;

        static String getMacroValue(String macroName) {

            try {
                switch (MacroCode.valueOf(macroName)) {
                    case _MC_:
                        return ClientMetadata.getInstance().getNetworkOperatorForUrl();
//                   case _MAC_:
//                       return ClientMetadata.getMacAddress();
//                   case _ANDROIDID_:
//                       return ClientMetadata.getInstance().getAndroidId();
//                   case _ANDROIDIDMD5_:
//                       return Md5Util.md5(ClientMetadata.getInstance().getAndroidId());
//                   case _GAID_:
//                       return ClientMetadata.getInstance().getAdvertisingId();
//                   case _GAIDMD5_:
//                       return Md5Util.md5(ClientMetadata.getInstance().getAdvertisingId());
//                   case _IMEI_:
//                       return ClientMetadata.getInstance().getDeviceId();
//                   case _IMEIMD5_:
//                       return Md5Util.md5(ClientMetadata.getInstance().getDeviceId());
//                   case _IMEI1_:
//                       return ClientMetadata.getInstance().getDeviceId(0);
//                   case _IMEI1MD5_:
//                       return Md5Util.md5(ClientMetadata.getInstance().getDeviceId(0));
//                   case _IMEI2_:
//                       return ClientMetadata.getInstance().getDeviceId(1);
//                   case _IMEI2MD5_:
//                       return Md5Util.md5(ClientMetadata.getInstance().getDeviceId(1));
//                   case _MACMD5_:
//                       return Md5Util.md5(ClientMetadata.getMacAddress());
                    case _COUNTRY_:
                        return ClientMetadata.getInstance().getDeviceLocale().getCountry();
                    case _BUNDLEID_:
                        return ClientMetadata.getInstance().getAppPackageName();
                    case _LANGUAGE_:
                        return ClientMetadata.getInstance().getDeviceLocale().getLanguage();
                    case _OSVERSION_:
                        return ClientMetadata.getDeviceOsVersion();
                    case _TIMESTAMP_:
                        return String.valueOf(System.currentTimeMillis() / 1000);
                    case _TIMEMILLIS_:
                        return String.valueOf(System.currentTimeMillis());
//                   case _OAID_:{
//                       return  ClientMetadata.getInstance().getOAID();
//                   }
                    default:
                        return "unFind";
                }
            } catch (Throwable e) {
                return "unFind";
            }

        }
    }

    public void setServerMacroMap(Map<String, String> map) {
        mServerMacroMap = map;
    }

    public void addMarcoKey(String marco, String str) {
        mMacroMap.put(marco, str);
    }

    public void addAllMarco(Map<String, String> map) {
        mMacroMap.putAll(map);
    }

    public Map<String, String> getMacroMap() {
        return mMacroMap;
    }

    public String getMarcoKey(String marco) {
        return mMacroMap.get(marco);
    }

    public void removeMarcoKey(String marco) {
        mMacroMap.remove(marco);
    }

    public void clearMacro() {
        mMacroMap.clear();
    }

    private String replaceMacroWithMap(String key, Map<String, String> extMap) {

        String value = mServerMacroMap.get(key);
        SigmobLog.d("macroProcess() called with:" + "[" + key + "]" + "[" + value + "]");
        if (!TextUtils.isEmpty(value) && !value.equals("unFind")) {
            return value;
        }
        value = mMacroMap.get(key);
        SigmobLog.d("macroProcess() called with:" + "[" + key + "]" + "[" + value + "]");
        if (!TextUtils.isEmpty(value) && !value.equals("unFind")) {
            return value;
        }
        if (extMap != null) {
            value = extMap.get(key);
            SigmobLog.d("macroProcess() called with:" + "[" + key + "]" + "[" + value + "]");
            if (!TextUtils.isEmpty(value) && !value.equals("unFind")) {
                return value;
            }
        }
        return null;
    }

    public String replaceWithDefault(String key) {
        String value = MacroCode.getMacroValue(key);
        SigmobLog.d("macroProcess() called with:" + "[" + key + "]" + "[" + value + "]");
        if (!TextUtils.isEmpty(value) && !value.equals("unFind")) {
            return value;
        }
        return null;
    }

    public String macroProcess(String url) {
        return this.macroProcess(url, null);
    }

    public String macroProcess(String url, Map<String, String> extMap) {

        if (TextUtils.isEmpty(url)) {
            return url;
        }

        String patt = "_([A-Z,0-9])+_";
        Pattern r = Pattern.compile(patt);
        Matcher m = r.matcher(url);

        String tempUrl = url;
        try {
            SigmobLog.d("macroProcess() called with: origin url " + url);
            while (m.find()) {
                String key = m.group();
                String value = replaceMacroWithMap(key, extMap);
                if (!TextUtils.isEmpty(value) && !value.equals("unFind")) {
                    tempUrl = tempUrl.replaceAll(key, value);
                } else {
                    value = replaceWithDefault(key);
                    if (!TextUtils.isEmpty(value) && !value.equals("unFind")) {
                        tempUrl = tempUrl.replaceAll(key, value);
                    }
                }
            }
            SigmobLog.d("macroProcess() called with: final url " + tempUrl);

        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }

        return tempUrl;
    }
}
