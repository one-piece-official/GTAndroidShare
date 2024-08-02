/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.czhj.volley;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A request dispatch queue with a thread pool of dispatchers.
 * <p>
 * <p>Calling will enqueue the given Request for dispatch, resolving from
 * either cache or network on a worker thread, and then delivering a parsed response on the main
 * thread.
 */

class VolleyThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {


        VolleyLog.d("Volley ThreadFactor create ,current thread num :" + Thread.activeCount());
        return new Thread(r) {
            @Override
            public void run() {
                // 设置线程的优先级
                super.run();
            }
        };
    }

}


public class RequestQueue {

    /**
     * Number of network request dispatcher threads to start.
     */
    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 4;
    /**
     * Used for generating monotonically-increasing sequence numbers for requests.
     */
    private final AtomicInteger mSequenceGenerator = new AtomicInteger();


    /**
     * The set of all requests currently being processed by this RequestQueue. A Request will be in
     * this set if it is waiting in any queue or currently being processed by any dispatcher.
     */
    private final Set<Request<?>> mCurrentRequests = new HashSet<>();

    private CustomLinkedBlockingQueue mRunableQueue = new CustomLinkedBlockingQueue();

    /**
     * Network interface for performing requests.
     */
    private final Network mNetwork;
    /**
     * Response delivery mechanism.
     */
    private final ResponseDelivery mDelivery;

    private final List<RequestFinishedListener> mFinishedListeners = new ArrayList<>();
    /**
     * The cache dispatcher.
     */
    private ThreadPoolExecutor mExecutorService = null;

    /**
     *
     */

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
     * @param network        A Network interface for performing HTTP requests
     * @param threadPoolSize Number of network dispatcher threads to create
     * @param delivery       A ResponseDelivery interface for posting responses and errors
     */

    public RequestQueue(Network network, int threadPoolSize, int maxPoolSize, ResponseDelivery delivery) {
        mNetwork = network;
        mDelivery = delivery;

        if (mExecutorService == null) {
            mExecutorService = new ThreadPoolExecutor(threadPoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, mRunableQueue, new VolleyThreadFactory());
            mRunableQueue.setExecutor(mExecutorService);
        }
    }

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
     * @param network        A Network interface for performing HTTP requests
     * @param threadPoolSize Number of network dispatcher threads to create
     */
    public RequestQueue(Network network, int threadPoolSize, int maxPoolSize) {
        this(network,
                threadPoolSize,
                maxPoolSize,
                new ExecutorDelivery(new Handler(Looper.getMainLooper())));
    }

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
     * @param network A Network interface for performing HTTP requests
     */
    public RequestQueue(Network network) {
        this(network, DEFAULT_NETWORK_THREAD_POOL_SIZE, Integer.MAX_VALUE);
    }

    /**
     * Starts the dispatchers in this queue.
     */
    public void start() {
        stop(); // Make sure any currently running dispatchers are stopped.
        // Create the cache dispatcher and start it.

        // Create network dispatchers (and corresponding threads) up to the pool size.
    }

    /**
     * Stops the cache and network dispatchers.
     */
    public void stop() {

    }

    /**
     * Gets a sequence number.
     */
    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }


    /**
     * Cancels all requests in this queue for which the given filter applies.
     *
     * @param filter The filtering function to use
     */
    public void cancelAll(RequestFilter filter) {
        synchronized (mCurrentRequests) {
            for (Request<?> request : mCurrentRequests) {
                if (filter.apply(request)) {
                    request.cancel();
                }
            }
        }
    }

    /**
     * Cancels all requests in this queue with the given tag. Tag must be non-null and equality is
     * by identity.
     */
    public void cancelAll(final Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancelAll with a null tag");
        }
        cancelAll(
                new RequestFilter() {
                    @Override
                    public boolean apply(Request<?> request) {
                        return request.getTag() == tag;
                    }
                });
    }

    /**
     * Adds a Request to the dispatch queue.
     *
     * @param request The request to service
     * @return The passed-in request
     */
    public <T> Request<T> add(Request<T> request) {

        if (request == null || TextUtils.isEmpty(request.getUrl()))
            return null;

        try {
//            VolleyLog.d("add request :" + request.getUrl()  + "mCurrentRequests "+ mCurrentRequests.size() + " mExecutorService "+ mExecutorService.toString());
            mExecutorService.submit(new NetworkDispatcher(mNetwork, request, mDelivery));
        }catch (Exception e){
            VolleyLog.e(e, "add request error");
        }

        return request;
    }

    /**
     * Called from {@link Request#finish(String)}, indicating that processing of the given request
     * has finished.
     */
    @SuppressWarnings("unchecked")
    // see above note on RequestFinishedListener
    <T> void finish(Request<T> request) {
        // Remove from the set of requests currently being processed.
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }
        synchronized (mFinishedListeners) {
            for (RequestFinishedListener<T> listener : mFinishedListeners) {
                listener.onRequestFinished(request);
            }
        }
    }

    public <T> void addRequestFinishedListener(RequestFinishedListener<T> listener) {
        synchronized (mFinishedListeners) {
            mFinishedListeners.add(listener);
        }
    }

    /**
     * Remove a RequestFinishedListener. Has no effect if listener was not previously added.
     */
    public <T> void removeRequestFinishedListener(RequestFinishedListener<T> listener) {
        synchronized (mFinishedListeners) {
            mFinishedListeners.remove(listener);
        }
    }

//    public int getThreadPoolSize() {
//        return mDispatchers.length;
//    }

    /**
     * Callback interface for completed requests.
     */
    // TODO: This should not be a generic class, because the request type can't be determined at
    // compile time, so all calls to onRequestFinished are unsafe. However, changing this would be
    // an API-breaking change. See also: https://github.com/google/volley/pull/109
    public interface RequestFinishedListener<T> {
        /**
         * Called when x``a request has finished processing.
         */
        void onRequestFinished(Request<T> request);
    }

    /**
     * A simple predicate or filter interface for Requests, for use by {@link
     * RequestQueue#cancelAll(RequestFilter)}.
     */
    public interface RequestFilter {
        boolean apply(Request<?> request);
    }

    static class CustomLinkedBlockingQueue extends LinkedBlockingQueue<Runnable> {
        private ThreadPoolExecutor executor;

        public CustomLinkedBlockingQueue() {
            super();
        }

        public void setExecutor(ThreadPoolExecutor executor) {
            this.executor = executor;
        }

        @Override
        public boolean offer(Runnable runnable) {
            // 判断当前活跃线程数是否小于最大线程数
            if (executor != null && executor.getActiveCount()>=executor.getPoolSize() && executor.getPoolSize() < executor.getMaximumPoolSize()) {
                return false;  // 强制线程池尝试创建新的线程
            }
            return super.offer(runnable);  // 否则，正常将任务加入队列
        }
    }
}


