package com.czhj.sdk.common.network;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.czhj.sdk.common.utils.Preconditions;
import com.czhj.sdk.logger.SigmobLog;
import com.czhj.volley.toolbox.HurlStack;

import org.json.JSONObject;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SigmobRequestUtil {


    /**
     * This is a helper class and should not be instantiated.
     */
    private SigmobRequestUtil() {
    }





    static boolean isSigmobHost(final String url) {

        try {
            HashSet urls = Networking.getSigmobServerURLS();
            return (urls != null && urls.contains(url));
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }

        return false;
    }


//    public static String convertRequestUrl(final String url) {
//
//        if (isSigmobHost(url)) {
//            if (url.indexOf('?') == -1)
//                return url + "?" + sigmobServerQueryString();
//            else {
//                return url + "&" + sigmobServerQueryString();
//
//            }
//        }
//        return url.trim();
//
//    }

    public static String truncateQueryParamsIfPost(final String url) {
        Preconditions.NoThrow.checkNotNull(url);

        final int queryPosition = url.indexOf('?');
        if (queryPosition == -1) {
            return url;
        }

        return url.substring(0, queryPosition);
    }


    public static Map<String, String> convertQueryToMap(final Context context,
                                                        final String url) {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(url);

        final Map<String, String> params = new HashMap<>();
        HurlStack.UrlRewriter rewriter = Networking.getUrlRewriter();
        final Uri uri = Uri.parse(rewriter.rewriteUrl(url));
        for (final String queryParam : uri.getQueryParameterNames()) {
            params.put(queryParam, TextUtils.join(",", uri.getQueryParameters(queryParam)));
        }

        return params;
    }


    public static String generateBodyFromParams(final Map<String, String> params,
                                                final String url) {
        Preconditions.NoThrow.checkNotNull(url);

        if (params == null || params.isEmpty()) {
            return null;
        }

        final JSONObject jsonBody = new JSONObject();
        for (final String queryName : params.keySet()) {
            try {
                jsonBody.put(queryName, params.get(queryName));
            } catch (Throwable e) {
                SigmobLog.d("Unable to add " + queryName + " to JSON body.");
            }
        }
        return jsonBody.toString();
    }

    public static boolean isConnection(String hostname) {
        return true;
    }


    private static class DNSResolver implements Runnable {
        private String domain;
        private InetAddress inetAddr;


        public DNSResolver(String domain) {
            this.domain = domain;
        }


        public void run() {
            try {
                InetAddress addr = InetAddress.getByName(domain);
                set(addr);
            } catch (Throwable e) {
            }
        }


        public synchronized void set(InetAddress inetAddr) {
            this.inetAddr = inetAddr;
        }

        public synchronized InetAddress get() {
            return inetAddr;
        }
    }


}

