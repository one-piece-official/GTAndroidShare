package com.czhj.sdk.common.track;

import android.os.Handler;

import com.czhj.sdk.common.Constants;
import com.czhj.sdk.common.ThreadPool.RepeatingHandlerRunnable;
import com.czhj.sdk.common.ThreadPool.ThreadPoolFactory;
import com.czhj.sdk.common.network.Networking;
import com.czhj.sdk.logger.SigmobLog;
import com.czhj.volley.NetworkResponse;
import com.czhj.volley.RequestQueue;
import com.czhj.volley.VolleyError;

import java.util.List;

public class TrackManager {

    private static final int MAX_PARALLELS_NUM = 4;
    private RepeatingHandlerRunnable repeatingHandlerRunnable;
    private final static int maxRetryNum = 20;
    private long retryExpiredTime = 180 * 1000;
    private long retryInterval = 10 * 1000;
    private Listener mToBidTrackListener;
    private Listener mSigmobTrackListener;
    private final static TrackManager gInstance = new TrackManager();
    private volatile int retryIndex;
    private List<AdTracker> trackers;
    private Handler retryHandler;

    public static TrackManager getInstance() {
        return gInstance;
    }

    private TrackManager() {

    }

    public void setRetryExpiredTime(long retryExpiredTime) {
        this.retryExpiredTime = retryExpiredTime * 1000;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval * 1000;
    }


    private void sendRetryTracking() {

        if (trackers != null && retryIndex < trackers.size()) {

            AdTracker adTracker = trackers.get(retryIndex++);

            final Listener listener = adTracker.getMessageType().equals(AdTracker.MessageType.TRACKING_URL) ? mSigmobTrackListener : mToBidTrackListener;
            if (listener == null) return;
            sendTracking(adTracker, null, false, true, new Listener() {
                @Override
                public void onSuccess(AdTracker tracker, NetworkResponse response) {
                    if (listener != null) {
                        listener.onSuccess(tracker, response);
                    }
                    retryHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            sendRetryTracking();
                        }
                    });
                }

                @Override
                public void onErrorResponse(AdTracker tracker, VolleyError error) {
                    if (listener != null) {
                        listener.onErrorResponse(tracker, error);
                    }

                    retryHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            sendRetryTracking();
                        }
                    });
                }
            });
        }

    }

    private void retryFailTracking() {

        retryHandler.removeCallbacksAndMessages(null);

        AdTracker.cleanLimitAdTracker(Constants.RETRYMAXNUM);
        trackers = AdTracker.getAdTrackerFromDB(Constants.RETRYMAXNUM, retryExpiredTime);
        retryIndex = 0;

        int parallel_size = trackers.size();
        if (parallel_size > MAX_PARALLELS_NUM) {
            parallel_size = MAX_PARALLELS_NUM;
        }

        for (int i = 0; i < parallel_size; i++) {
            sendRetryTracking();
        }

        AdTracker.cleanExpiredAdTracker(retryExpiredTime);

    }


    public interface Listener {
        void onSuccess(AdTracker tracker, NetworkResponse response);

        void onErrorResponse(AdTracker tracker, VolleyError error);

    }


    public void setSigmobTrackListener(Listener listener) {
        mSigmobTrackListener = listener;
    }

    public void setToBidTrackListener(Listener listener) {
        mToBidTrackListener = listener;
    }

    public static void sendTracking(final AdTracker tracker, BaseMacroCommon macroCommon, final boolean isMulti, final boolean inQueue, final Listener listener) {

        if (tracker != null) {

            if (tracker.getMessageType() != AdTracker.MessageType.QUARTILE_EVENT && (!tracker.isTracked() || isMulti)) {

                String trackingUrl = tracker.getUrl();
                if (macroCommon != null) {
                    trackingUrl = macroCommon.macroProcess(trackingUrl);
                }

                if (!isMulti) {
                    tracker.setTracked();
                }

                final String finalUrlString = trackingUrl;
                final boolean isRetrySend = tracker.getId() != null;


                tracker.setUrl(finalUrlString);
                TrackingRequest request = new TrackingRequest(finalUrlString, isRetrySend ? 0 : tracker.getRetryNum(), new TrackingRequest.RequestListener() {
                    @Override
                    public void onSuccess(NetworkResponse response) {


                        if (isRetrySend) {

                            ThreadPoolFactory.MainThreadRun(new Runnable() {
                                @Override
                                public void run() {
                                    tracker.deleteDB();
                                }
                            });
                        }

                        if (listener != null) {
                            listener.onSuccess(tracker, response);
                        }

                    }

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        NetworkResponse response = error.networkResponse;

                        if (inQueue) {

                            if (tracker.getRetryNum() > 0 || isRetrySend) {
                                ThreadPoolFactory.MainThreadRun(new Runnable() {
                                    @Override
                                    public void run() {

                                        if (!isRetrySend) {

                                            tracker.insertToDB(null);
                                        } else {

                                            tracker.setRetryCountInc();
                                            if (tracker.getRetryCount() >= maxRetryNum) {
                                                tracker.deleteDB();
                                            } else {
                                                tracker.updateToDB();
                                            }
                                        }
                                    }
                                });
                            }
                        }
                        if (listener != null) {
                            listener.onErrorResponse(tracker, error);
                        }

                        SigmobLog.e(error.getMessage());
                    }
                });

                RequestQueue commonQueue = Networking.getCommonRequestQueue();
                RequestQueue retryQueue = Networking.getAdTrackerRetryQueue();

                if (commonQueue == null && retryQueue == null) {
                    SigmobLog.e("RequestQueue is null");
                    return;
                }

                RequestQueue targetQueue;
                if (isRetrySend) {
                    targetQueue = (retryQueue != null) ? retryQueue : commonQueue;
                } else {
                    targetQueue = (commonQueue != null) ? commonQueue : retryQueue;
                }

                targetQueue.add(request);

            }
        }

    }

    public void startRetryTracking() {

        if (repeatingHandlerRunnable != null) return;


        retryHandler = new Handler(ThreadPoolFactory.BackgroundThreadPool.getInstance().getIOLooper());

        repeatingHandlerRunnable = new RepeatingHandlerRunnable(retryHandler) {
            @Override
            protected void doWork() {
                try {

                    retryFailTracking();
                    repeatingHandlerRunnable.startRepeating(retryInterval);

                } catch (Throwable throwable) {
                    SigmobLog.e("retryFaildTracking error " + throwable.getMessage());
                }
            }
        };

        repeatingHandlerRunnable.startRepeating(retryInterval);

    }

}
