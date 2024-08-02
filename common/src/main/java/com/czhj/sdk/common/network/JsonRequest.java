package com.czhj.sdk.common.network;

import com.czhj.sdk.logger.SigmobLog;
import com.czhj.volley.DefaultRetryPolicy;
import com.czhj.volley.NetworkResponse;
import com.czhj.volley.ParseError;
import com.czhj.volley.Response;
import com.czhj.volley.VolleyError;
import com.czhj.volley.toolbox.HttpHeaderParser;

import org.json.JSONObject;

public class JsonRequest extends SigmobRequest<JSONObject> {

    private static final int ZERO_RETRIES = 0;
    private final JsonRequest.Listener mListener;
    protected static final String PROTOCOL_CHARSET = "utf-8";


    public interface Listener extends Response.ErrorListener {
        void onSuccess(JSONObject response);
    }

    public JsonRequest(String url, Listener mListener, int retryNum) {
        super(url, Method.GET, mListener);
        this.mListener = mListener;


        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);
        setShouldCache(false);

    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {

        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
            return Response.error(new ParseError(e));

        }

    }


    @Override
    public void deliverError(VolleyError error) {
        SigmobLog.e("send tracking: " + getUrl() + " fail");

        super.deliverError(error);

    }

    @Override
    protected void deliverResponse(JSONObject response) {
        JsonRequest.Listener listener;
        synchronized (mLock) {
            listener = mListener;
        }

        SigmobLog.i("send tracking: " + getUrl() + " success");
        if (listener != null) {
            listener.onSuccess(response);
        }
    }

}
