package com.czhj.volley.toolbox;

import android.text.TextUtils;

import com.czhj.volley.AuthFailureError;
import com.czhj.volley.DefaultRetryPolicy;
import com.czhj.volley.Header;
import com.czhj.volley.Request;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttp3Stack extends BaseHttpStack {
    public static final String NETWORK_TIMEOUT = "NETWORK_TIMEOUT";

    private final SSLSocketFactory mSslSocketFactory;
    private final OkHttpClient.Builder clientBuilder;
    private final OkHttpClient client;

    Interceptor timeoutInterceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            try {
                okhttp3.Request request = chain.request();

                int connectTimeout = chain.connectTimeoutMillis();
                int readTimeout = chain.readTimeoutMillis();
                int writeTimeout = chain.writeTimeoutMillis();

                String connectNew = request.header(NETWORK_TIMEOUT);

                if (!TextUtils.isEmpty(connectNew)) {
                    connectTimeout = Integer.valueOf(connectNew);
                }


                return chain
                        .withConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .withReadTimeout(readTimeout, TimeUnit.MILLISECONDS)
                        .withWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                        .proceed(request);
            }catch (Throwable th){

            }
           return chain.proceed(chain.request());
        }
    };
    public OkHttp3Stack() {
        clientBuilder = new OkHttpClient.Builder();
        clientBuilder.connectionPool(new ConnectionPool());
        clientBuilder.connectTimeout(DefaultRetryPolicy.DEFAULT_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        clientBuilder.readTimeout(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        clientBuilder.writeTimeout(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        clientBuilder.addInterceptor(timeoutInterceptor);
        mSslSocketFactory = null;
        client = clientBuilder.build();
    }

    public OkHttp3Stack(SSLSocketFactory socketFactory) {
        clientBuilder = new OkHttpClient.Builder();
        clientBuilder.connectionPool(new ConnectionPool());
        clientBuilder.connectTimeout(DefaultRetryPolicy.DEFAULT_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        clientBuilder.readTimeout(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        clientBuilder.writeTimeout(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        clientBuilder.addInterceptor(timeoutInterceptor);
        mSslSocketFactory = socketFactory;
        client = clientBuilder.build();
    }

    private static void setConnectionParametersForRequest(okhttp3.Request.Builder builder, Request<?> request)
            throws AuthFailureError {
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                // Ensure backwards compatibility.  Volley assumes a request with a null body is a GET.
                byte[] postBody = request.getBody();
                if (postBody != null) {
                    builder.post(RequestBody.create(MediaType.parse(request.getBodyContentType()), postBody));
                }
                break;
            case Request.Method.GET:
                builder.get();
                break;
            case Request.Method.DELETE:
                builder.delete(createRequestBody(request));
                break;
            case Request.Method.POST:
                builder.post(createRequestBody(request));
                break;
            case Request.Method.PUT:
                builder.put(createRequestBody(request));
                break;
            case Request.Method.HEAD:
                builder.head();
                break;
            case Request.Method.OPTIONS:
                builder.method("OPTIONS", null);
                break;
            case Request.Method.TRACE:
                builder.method("TRACE", null);
                break;
            case Request.Method.PATCH:
                builder.patch(createRequestBody(request));
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private static RequestBody createRequestBody(Request r) throws AuthFailureError {
        final byte[] body = r.getBody();
        if (body == null) {
            return null;
        }
        return RequestBody.create(MediaType.parse(r.getBodyContentType()), body);
    }


    @Override
    public HttpResponse executeRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {

        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder();
        okHttpRequestBuilder.url(request.getUrl());

        Map<String, String> headers = request.getHeaders();

        okHttpRequestBuilder.addHeader(NETWORK_TIMEOUT, request.getRetryPolicy().getCurrentTimeout() + "");
        for (final String name : headers.keySet()) {

            String value = headers.get(name);
            if (TextUtils.isEmpty(value)) {
                okHttpRequestBuilder.removeHeader(name);
            } else {
                okHttpRequestBuilder.addHeader(name, value);
            }
        }
        for (final String name : additionalHeaders.keySet()) {
            String value = additionalHeaders.get(name);
            if (TextUtils.isEmpty(value)) {
                okHttpRequestBuilder.removeHeader(name);
            } else {
                okHttpRequestBuilder.addHeader(name, value);
            }
        }

        setConnectionParametersForRequest(okHttpRequestBuilder, request);


        okhttp3.Request okHttpRequest = okHttpRequestBuilder.build();
        Call okHttpCall = client.newCall(okHttpRequest);
        Response okHttpResponse = okHttpCall.execute();


        int code = okHttpResponse.code();
        ResponseBody body = okHttpResponse.body();
        InputStream content = body == null ? null : body.byteStream();
        int contentLength = body == null ? 0 : (int) body.contentLength();
        List<Header> responseHeaders = mapHeaders(okHttpResponse.headers());
        return new HttpResponse(code, responseHeaders, contentLength, content);
    }

    private List<Header> mapHeaders(Headers responseHeaders) {
        List<Header> headers = new ArrayList<>();
        for (int i = 0, len = responseHeaders.size(); i < len; i++) {
            final String name = responseHeaders.name(i), value = responseHeaders.value(i);
            if (name != null) {
                headers.add(new Header(name, value));
            }
        }
        return headers;
    }
}