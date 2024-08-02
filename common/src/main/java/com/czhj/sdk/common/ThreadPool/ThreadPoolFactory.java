package com.czhj.sdk.common.ThreadPool;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ThreadPoolFactory {

    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private static Handler mHandler = new Handler(Looper.getMainLooper());
    private static Handler mIOHandler;

    private static ExecutorService mFixIOExecutor = Executors.newFixedThreadPool(NUMBER_OF_CORES + 1, new BackgroundThreadFactory());

    public static void MainThreadRun(Runnable runnable) {
        mHandler.post(runnable);
    }

    public static ExecutorService getFixIOExecutor() {
        return mFixIOExecutor;
    }

    private static HandlerThread mIOHandlerThread;

    public static class BackgroundThreadPool {

        final int KEEP_ALIVE_TIME = 2;
        final BlockingQueue<Runnable> taskQueue;
        final ExecutorService executorService;

        BackgroundThreadPool() {
            taskQueue = new LinkedBlockingQueue<>();
            executorService = mFixIOExecutor;
            mIOHandlerThread = new HandlerThread("ioThread");
            mIOHandlerThread.start();
            mIOHandler = new Handler(mIOHandlerThread.getLooper());
        }

        public Handler getIOHandler() {
            return mIOHandler;
        }

        public Looper getIOLooper() {

            return mIOHandlerThread.getLooper();
        }


        public ExecutorService getExecutorService() {
            return executorService;
        }

        private static BackgroundThreadPool gInstance = null;

        public static BackgroundThreadPool getInstance() {

            if (gInstance == null) {
                synchronized (BackgroundThreadPool.class) {
                    if (gInstance == null) {
                        gInstance = new BackgroundThreadPool();
                    }
                }
            }
            return gInstance;
        }


        public void submit(Runnable r) {
            try {
                executorService.submit(r);

            } catch (Throwable throwable) {

            }
        }
    }

}
