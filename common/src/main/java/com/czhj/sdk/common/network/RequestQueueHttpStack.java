package com.czhj.sdk.common.network;

import com.czhj.volley.AuthFailureError;
import com.czhj.volley.Request;
import com.czhj.volley.toolbox.BaseHttpStack;
import com.czhj.volley.toolbox.HttpResponse;
import com.czhj.volley.toolbox.HurlStack;
import com.czhj.volley.toolbox.OkHttp3Stack;
import com.czhj.volley.toolbox.Volley;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.SSLSocketFactory;

class RequestQueueHttpStack extends BaseHttpStack {

    BaseHttpStack httpStack;

    private RequestQueueHttpStack() {

        boolean enableOkhttp3 = Volley.isEnableOkhttp3();
        if (enableOkhttp3) {
            try {
                httpStack = new OkHttp3Stack();
            } catch (Throwable e) {

            }
        }
        if (httpStack == null) {
            httpStack = new HurlStack();
        }
    }

    public RequestQueueHttpStack(final SSLSocketFactory sslSocketFactory) {
        boolean enableOkhttp3 = Volley.isEnableOkhttp3();
        if (enableOkhttp3) {
            try {
                httpStack = new OkHttp3Stack(sslSocketFactory);
            } catch (Throwable e) {

            }
        }

        if (httpStack == null) {
            httpStack = new HurlStack(null, sslSocketFactory);
        }
    }

    @Override
    public HttpResponse executeRequest(final Request<?> request,
                                       Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        // If the headers map is null or empty, make a new once since Collections.emptyMap()
        // returns an unmodifiable map.
        if (additionalHeaders == null || additionalHeaders.isEmpty()) {
            additionalHeaders = new TreeMap<>();
        }

        additionalHeaders.put(ResponseHeader.USER_AGENT.getKey(), Networking.getUserAgent());

        return httpStack.executeRequest(request, additionalHeaders);
    }
}
