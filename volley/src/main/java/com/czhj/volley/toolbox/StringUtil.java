package com.czhj.volley.toolbox;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Create by lance on 2020/3/13/0013
 */
public class StringUtil {

//    public static String service = "adservice.sigmob.cn";
//    public static String dc = "dc.sigmob.cn";
//    public static String track = "track.sigmob.cn";
//    public static String tp = "tracktp.sigmob.cn";
//    public static String cn = "n.sigmob.cn";
//    public static String rtb = "rtbcallback.sigmob.cn";

    public static String service = "dgvhuylfh1vljpre1fq";//adservice.sigmob.cn
    public static String dc = "gf1vljpre1fq";//dc.sigmob.cn
    public static String track = "wudfn1vljpre1fq";//track.sigmob.cn
    public static String tp = "wudfnws1vljpre1fq";//tracktp.sigmob.cn
    public static String cn = "q1vljpre1fq";//n.sigmob.cn
    public static String rtb = "uwefdooedfn1vljpre1fq";//rtbcallback.sigmob.cn
    public static String sad= "vljpreDg"; //sigmobad
    public static String s = "vljpre"; //sigmob

    public static Map<String, String> urlMap = new HashMap();
    public static String clsName = "frp1f}km";

    static {
        urlMap.put(decode(service), "adservice.sigmob.cn");
        urlMap.put(decode(dc), "dc.sigmob.cn");
        urlMap.put(decode(track), "track.sigmob.cn");
        urlMap.put(decode(tp), "tracktp.sigmob.cn");
        urlMap.put(decode(cn), "n.sigmob.cn");
        urlMap.put(decode(rtb), "rtbcallback.sigmob.cn");
    }

    public static  String scheme(){
        return decode(sad);
    }

    public static String getUrl(String url){
        String name = StringUtil.class.getName();

        if(name.startsWith(decode(clsName))){
            return url;
        }

        for (Map.Entry<String, String> entry : StringUtil.urlMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!TextUtils.isEmpty(url) && url.contains(key)) {
                return url.replace(key, value);

            }
        }
        return url;
    }
//    public static String encode(String encode) {
//        byte[] arrayOfByte = encode.getBytes();
//        for (int i = 0; i < arrayOfByte.length; i++) {
//            arrayOfByte[i] = ((byte) (arrayOfByte[i] + 3));
//        }
//        return new String(arrayOfByte);
//    }


    public static String decode(String decode) {
        byte[] arrayOfByte = decode.getBytes();
        for (int i = 0; i < arrayOfByte.length; i++) {
            arrayOfByte[i] = ((byte) (arrayOfByte[i] - 3));
        }
        return new String(arrayOfByte);
    }
}
