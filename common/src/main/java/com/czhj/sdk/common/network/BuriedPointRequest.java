package com.czhj.sdk.common.network;

import android.util.Base64;

import com.czhj.sdk.common.models.Config;
import com.czhj.sdk.common.utils.AESUtil;
import com.czhj.sdk.logger.SigmobLog;
import com.czhj.volley.DefaultRetryPolicy;
import com.czhj.volley.NetworkResponse;
import com.czhj.volley.Response;
import com.czhj.volley.VolleyError;

import java.util.Map;

public class BuriedPointRequest extends SigmobRequest<NetworkResponse> {


    private final RequestListener mListener;

    private byte[] bodyBytes = null;
    private boolean isEnc;

    private BuriedPointRequest(final String url, final String body, RequestListener mListener) {
        super(url, Method.POST, null);
        this.mListener = mListener;
        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);

        setShouldCache(false);


        try {
            if (Config.sharedInstance().isEnc()) {
                bodyBytes = AESUtil.Encrypt(body.getBytes(), SigmobRequest.AESKEY);
                isEnc = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!isEnc) {
                bodyBytes = body.getBytes();
            }
        }
    }


    public static void BuriedPointSend(String body, RequestListener requestListener) {
        if (body == null || body.length() == 0) {

            if (requestListener != null) {
                requestListener.onErrorResponse(new VolleyError("body is empty"));
            }
        }

        if (Networking.getBuriedPointRequestQueue() == null) {
            if (requestListener != null) {
                requestListener.onErrorResponse(new VolleyError("BuriedPointRequestQueue is empty"));
            }
            return;
        }

        try {
            BuriedPointRequest request = new BuriedPointRequest(Config.sharedInstance().getLogUrl(), body, requestListener);
            Networking.getBuriedPointRequestQueue().add(request);
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
            if (requestListener != null)
                requestListener.onErrorResponse(new VolleyError("network is disconnect "));
        }
    }

    @Override
    public int getMaxLength() {
        return 100;
    }

    @Override
    public Map<String, String> getHeaders() {

        Map<String, String> headers = super.getHeaders();
        if (isEnc) {
            byte[] bytes = AESUtil.generateNonce();
            headers.put("agn", Base64.encodeToString(bytes, Base64.NO_WRAP));
        }
        headers.put("gz", "on");
        return headers;

    }


    @Override
    public byte[] getBody() {
        return bodyBytes;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, null);
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        RequestListener listener;
        synchronized (mLock) {
            listener = mListener;
        }

        SigmobLog.d("send dclog: " + getUrl() + " success");
        if (listener != null) {
            listener.onSuccess();
        }
    }

    @Override
    public void deliverError(VolleyError error) {
        RequestListener listener;
        synchronized (mLock) {
            listener = mListener;
        }

        SigmobLog.i("send dclog: " + getUrl() + " onErrorResponse");
        if (listener != null) {
            listener.onErrorResponse(error);
        }
    }

    public interface RequestListener {
        void onSuccess();

        void onErrorResponse(VolleyError error);
    }
}
