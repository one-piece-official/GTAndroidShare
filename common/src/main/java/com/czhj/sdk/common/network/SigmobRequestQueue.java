package com.czhj.sdk.common.network;

import android.os.Handler;

import com.czhj.sdk.common.ThreadPool.ThreadPoolFactory;
import com.czhj.volley.ExecutorsDelivery;
import com.czhj.volley.Network;
import com.czhj.volley.Request;
import com.czhj.volley.RequestQueue;
import com.czhj.volley.ResponseDelivery;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SigmobRequestQueue extends RequestQueue {

    private static final int CAPACITY = 10;


    private final Map<Request<?>, DelayedRequestHelper> mDelayedRequests;

    SigmobRequestQueue(Network network, int threadPoolSize, int maxThreads) {
        this(network, threadPoolSize, maxThreads, new ExecutorsDelivery(ThreadPoolFactory.getFixIOExecutor()));
    }

    SigmobRequestQueue(Network network, int threadPoolSize, int maxThreads, ResponseDelivery responseDelivery) {
        super(network, threadPoolSize, maxThreads, responseDelivery);
        mDelayedRequests = new HashMap<>(CAPACITY);
    }

    SigmobRequestQueue(Network network) {
        super(network);
        mDelayedRequests = new HashMap<>(CAPACITY);
    }

    /**
     * Convenience method for adding a request with a time delay to the request queue.
     *
     * @param request The request.
     * @param delayMs The delay in ms for adding the request to the request queue.
     */
    public void addDelayedRequest(Request<?> request, int delayMs) {
        addDelayedRequest(request, new DelayedRequestHelper(request, delayMs));
    }


    private void addDelayedRequest(Request<?> request, DelayedRequestHelper delayedRequestHelper) {

        if (mDelayedRequests.containsKey(request)) {
            cancel(request);
        }

        delayedRequestHelper.start();
        mDelayedRequests.put(request, delayedRequestHelper);
    }

    /**
     * Override of cancelAll method to ensure delayed requests are cancelled as well.
     */
    public void cancelAll() {

        this.cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    /**
     * Override of cancelAll method to ensure delayed requests are cancelled as well.
     */
    @Override
    public void cancelAll(RequestFilter filter) {

        super.cancelAll(filter);

        Iterator<Map.Entry<Request<?>, DelayedRequestHelper>> iterator = mDelayedRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Request<?>, DelayedRequestHelper> entry = iterator.next();
            if (filter.apply(entry.getKey())) {
                // Here we cancel both the request and the handler from posting the delayed runnable
                entry.getKey().cancel();
                entry.getValue().cancel();
                iterator.remove();
            }
        }
    }

    /**
     * Override of cancelAll method to ensure delayed requests are cancelled as well.
     */
    @Override
    public void cancelAll(final Object tag) {

        super.cancelAll(tag);

        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return request.getTag() == tag;
            }
        });
    }

    /**
     * Convenience method to cancel a single request.
     *
     * @param request The request to cancel.
     */
    private void cancel(final Request<?> request) {

        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> _request) {
                return request == _request;
            }
        });
    }


    @Deprecated
    Map<Request<?>, DelayedRequestHelper> getDelayedRequests() {
        return mDelayedRequests;
    }

    /**
     * This helper class is used to package the supporting objects a request needs to
     * run at a delayed time and cancel if needed.
     */
    class DelayedRequestHelper {
        final int mDelayMs;

        final Handler mHandler;

        final Runnable mDelayedRunnable;

        DelayedRequestHelper(final Request<?> request, int delayMs) {
            this(request, delayMs, new Handler());
        }


        DelayedRequestHelper(final Request<?> request, int delayMs, Handler handler) {
            mDelayMs = delayMs;
            mHandler = handler;
            mDelayedRunnable = new Runnable() {
                @Override
                public void run() {
                    mDelayedRequests.remove(request);
                    SigmobRequestQueue.this.add(request);
                }
            };
        }

        void start() {
            mHandler.postDelayed(mDelayedRunnable, mDelayMs);
        }

        void cancel() {
            mHandler.removeCallbacks(mDelayedRunnable);
        }
    }
}

