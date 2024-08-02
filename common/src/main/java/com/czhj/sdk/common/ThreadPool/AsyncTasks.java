package com.czhj.sdk.common.ThreadPool;

import android.os.AsyncTask;

import com.czhj.sdk.common.utils.Preconditions;

import java.util.concurrent.Executor;

public class AsyncTasks {
    private static Executor sExecutor;

    static {
        init();
    }

    // This is in a separate method rather than a static block to pass lint.
    private static void init() {
        // Reuse the async task executor
        sExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    /**
     * Starting with Honeycomb, default AsyncTask#execute behavior runs the tasks serially. This
     * method attempts to force these AsyncTasks to run in parallel with a ThreadPoolExecutor.
     */
    @SafeVarargs
    public static <P> void safeExecuteOnExecutor(AsyncTask<P, ?, ?> asyncTask, P... params) {
        Preconditions.NoThrow.checkNotNull(asyncTask, "Unable to execute null AsyncTask.");
        Preconditions.NoThrow.checkUiThread("AsyncTask must be executed on the main thread");

        asyncTask.executeOnExecutor(sExecutor, params);
    }
}
