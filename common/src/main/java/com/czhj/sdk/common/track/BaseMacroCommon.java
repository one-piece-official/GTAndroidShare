package com.czhj.sdk.common.track;


import android.text.TextUtils;

import com.czhj.sdk.common.ClientMetadata;
import com.czhj.sdk.common.network.Networking;
import com.czhj.sdk.logger.SigmobLog;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseMacroCommon implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> mMacroMap = new HashMap<>();

    private Map<String, String> mServerMacroMap = new HashMap<>();

    public enum MacroCode {

        _USERAGENT_, _ORIGINTIME_, _ORIGIN_TIME_S_, _LOCALTIMESTAMP_;

        static String getMacroValue(String macroName) {
            try {
                switch (MacroCode.valueOf(macroName)) {
                    case _USERAGENT_:
                        return Networking.getUserAgent();
                    case _LOCALTIMESTAMP_:
                    case _ORIGIN_TIME_S_:
                        return String.valueOf(System.currentTimeMillis() / 1000);
                    case _ORIGINTIME_:
                        return String.valueOf(System.currentTimeMillis());
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
