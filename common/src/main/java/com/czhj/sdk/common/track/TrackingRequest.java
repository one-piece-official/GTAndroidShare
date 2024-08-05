package com.czhj.sdk.common.track;

import com.czhj.sdk.common.models.Config;
import com.czhj.sdk.logger.SigmobLog;
import com.czhj.volley.DefaultRetryPolicy;
import com.czhj.volley.NetworkResponse;
import com.czhj.volley.Request;
import com.czhj.volley.Response;
import com.czhj.volley.VolleyError;

public class TrackingRequest extends Request<NetworkResponse> {

    private static final int ZERO_RETRIES = 0;
    private static final int reTryFailCount = 0;
    private final RequestListener mListener;

    public TrackingRequest(String url, int retryNum, RequestListener mListener) {
        this(url, retryNum, Config.sharedInstance().getNetworkTimeout(), mListener);
    }

    public TrackingRequest(String url, int retryNum, int ConnectTimeoutMs, RequestListener mListener) {
        super(Method.GET, url, mListener);
        this.mListener = mListener;

        if (retryNum < 0) retryNum = 0;
        else if (retryNum > 3) retryNum = 3;

        setShouldRetryServerErrors(true);
        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(ConnectTimeoutMs, ConnectTimeoutMs, retryNum, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);
        setShouldCache(false);
    }

    @Override
    public int getMaxLength() {
        return 100;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, null);
    }

    @Override
    public void deliverError(VolleyError error) {
        SigmobLog.e("send tracking: " + getUrl() + " fail");
        super.deliverError(error);
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        RequestListener listener;
        synchronized (mLock) {
            listener = mListener;
        }

        SigmobLog.i("send tracking: " + getUrl() + " success");
        if (listener != null) {
            listener.onSuccess(response);
        }
    }

    public interface RequestListener extends Response.ErrorListener {
        void onSuccess(NetworkResponse response);
    }
}
