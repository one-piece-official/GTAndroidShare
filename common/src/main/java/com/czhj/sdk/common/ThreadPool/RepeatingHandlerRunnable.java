package com.czhj.sdk.common.ThreadPool;

import android.os.Handler;

import com.czhj.sdk.common.utils.Preconditions;

public abstract class RepeatingHandlerRunnable implements Runnable {

    private final Handler mHandler;
    private volatile boolean mIsRunning;
    protected volatile long mUpdateIntervalMillis;

    protected RepeatingHandlerRunnable( final Handler handler) {
        Preconditions.NoThrow.checkNotNull(handler);
        mHandler = handler;
    }

    @Override
    public void run() {
        if (mIsRunning) {
            doWork();
            mHandler.postDelayed(this, mUpdateIntervalMillis);
        }
    }

    protected abstract void doWork();

    /**
     * Start this runnable immediately, repeating at the provided interval.
     */
    public void startRepeating(long intervalMillis) {
        Preconditions.NoThrow.checkArgument(intervalMillis > 0, "intervalMillis must be greater than 0. " +
                "Saw: "+intervalMillis);
        mUpdateIntervalMillis = intervalMillis;
        if (!mIsRunning) {
            mIsRunning = true;
            mHandler.post(this);
        }
    }

    /**
     * Stop this repeating runnable.
     */
    public void stop() {
        mIsRunning = false;
        mHandler.removeCallbacksAndMessages(null);
    }


}
