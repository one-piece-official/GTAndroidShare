package com.czhj.sdk.common.network;

import android.text.TextUtils;

import com.czhj.sdk.common.ClientMetadata;
import com.czhj.volley.DefaultRetryPolicy;
import com.czhj.volley.NetworkResponse;
import com.czhj.volley.Request;
import com.czhj.volley.Response;
import com.czhj.volley.VolleyError;
import com.czhj.volley.toolbox.HttpHeaderParser;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public abstract class SigmobRequest<T> extends Request<T> {


    protected static final String AESKEY = "KGpfzbYsn4T9Jyuq";
    private final String mOriginalUrl;

    protected SigmobRequest(final String url,
                            final int method,
                            final Response.ErrorListener listener) {


        super(method, url, listener);

        mOriginalUrl = url;
        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);
        setShouldCache(false);
    }

    public String getOriginalUrl() {
        return mOriginalUrl;
    }

    @Override
    public String getBodyContentType() {

        return super.getBodyContentType();
    }

    @Override
    public String getUrl() {

        return super.getUrl();
    }

    @Override
    public Map<String, String> getHeaders() {
        TreeMap<String, String> headers = new TreeMap<>();

        if (!SigmobRequestUtil.isSigmobHost(mOriginalUrl)) {
            return headers;
        }


        // Use default locale first for language code
        String languageCode = Locale.getDefault().getLanguage();

        String udid = null;
//         If user's preferred locale is different from default locale, override language code
        if (ClientMetadata.getInstance() != null) {
            Locale userLocale = ClientMetadata.getInstance().getDeviceLocale();

            if (!userLocale.getLanguage().trim().isEmpty()) {
                languageCode = userLocale.getLanguage().trim();
            }
//
//            if (!TextUtils.isEmpty(ClientMetadata.getInstance().getAndroidId())) {
//                udid = Md5Util.md5(ClientMetadata.getInstance().getAndroidId());
//            }
        }

        // Do not add header if language is empty
        if (!TextUtils.isEmpty(languageCode)) {
            headers.put(ResponseHeader.ACCEPT_LANGUAGE.getKey(), languageCode);
        }

//        if (!TextUtils.isEmpty(udid)) {
//            headers.put(Constants.TOKEN, udid);
//        }


        return headers;
    }

    @Override
    public byte[] getBody() {
        final String body = SigmobRequestUtil.generateBodyFromParams(getParams(), getUrl());
        if (body == null) {
            return null;
        }

        return body.getBytes();
    }


    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        return (Response<T>) Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
        return super.parseNetworkError(volleyError);
    }

}

