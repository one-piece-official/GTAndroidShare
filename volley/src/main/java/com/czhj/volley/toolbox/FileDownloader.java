package com.czhj.volley.toolbox;

import android.os.Looper;

import com.czhj.volley.RequestQueue;

import java.util.LinkedList;
import java.util.List;

public class FileDownloader {
    /** RequestQueue for dispatching DownloadRequest. */
    final RequestQueue mRequestQueue;

    /** The parallel task count, recommend less than 3. */
    private final int mParallelTaskCount;

    /** The linked Task Queue. */
    private final LinkedList<DownloadController> mTaskQueue;

    public FileDownloader(RequestQueue queue) {
        this(queue, 0);
    }
    /**
     * Construct Downloader and init the Task Queue.
     * @param queue The RequestQueue for dispatching Download task.
     * @param parallelTaskCount
     * 				Allows parallel task count,
     * 				don't forget the value must less than ThreadPoolSize of the RequestQueue.
     */
    public FileDownloader(RequestQueue queue, int parallelTaskCount) {
//        if (parallelTaskCount >= queue.getThreadPoolSize()) {
//            throw new IllegalArgumentException("parallelTaskCount[" + parallelTaskCount
//                    + "] must less than threadPoolSize[" + queue.getThreadPoolSize() + "] of the RequestQueue.");
//        }

        mTaskQueue = new LinkedList<DownloadController>();
        mParallelTaskCount = parallelTaskCount;
        mRequestQueue = queue;
    }

    /**
     * Create a new download request, this request might not run immediately because the parallel task limitation,
     * you can check the status by the {@link DownloadController} which you got after invoke this method.
     *
     * Note: don't perform this method twice or more with same parameters, because we didn't check for
     * duplicate tasks, it rely on developer done.
     *
     * Note: this method should invoke in the main thread.
     *
     * @param item The download url.
     * @param listener The event callback by status;
     * @return The task controller allows pause or resume or discard operation.
     */
    public DownloadController add(DownloadItem item, FileDownloadRequest.FileDownloadListener listener) {
        // only fulfill requests that were initiated from the main thread.(reason for the Delivery?)

        DownloadController controller = get(item.filePath,item.url);
        if (controller == null){
            controller= new DownloadController(item, listener);
            synchronized (mTaskQueue) {
                mTaskQueue.add(controller);
            }
            schedule();
        }

        return controller;
    }


    /**
     * Scanning the Task Queue, fetch a {@link DownloadController} who match the two parameters.
     * @param storeFilePath The storeFilePath to compare.
     * @param url The url to compare.
     * @return The matched {@link DownloadController}.
     */
    public DownloadController get(String storeFilePath, String url) {
        synchronized (mTaskQueue) {
            for (DownloadController controller : mTaskQueue) {
                if (controller.getStorePath().equals(storeFilePath)
                        && controller.getUrl().equals(url)) return controller;
            }
        }
        return null;
    }

    /**
     * Traverse the Task Queue, count the running task then deploy more if it can be.
     */
    void schedule() {
        // make sure only one thread can manipulate the Task Queue.
        synchronized (mTaskQueue) {
            // counting ran task.
            int parallelTaskCount = 0;
            for (DownloadController controller : mTaskQueue) {
                if (controller.isDownloading()) parallelTaskCount++;
            }
            if (parallelTaskCount >= mParallelTaskCount) return;

            // try to deploy all Task if they're await.
            for (DownloadController controller : mTaskQueue) {
                if (controller.deploy() && ++parallelTaskCount == mParallelTaskCount) return;
            }
        }
    }

    /**
     * Remove the controller from the Task Queue, re-schedule to make those waiting task deploys.
     * @param controller The controller which will be remove.
     */
    void remove(DownloadController controller) {
        // also make sure one thread operation
        synchronized (mTaskQueue) {
            mTaskQueue.remove(controller);
        }
        schedule();
    }

    /**
     * Clear all tasks, make the Task Queue empty.
     */
    public void clearAll() {
        // make sure only one thread can manipulate the Task Queue.
        synchronized (mTaskQueue) {
            while (mTaskQueue.size() > 0) {
                mTaskQueue.get(0).discard();
            }
        }
    }

    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("FileDownloader must be invoked from the main thread.");
        }
    }

    /**
     * This method can override by developer to change download behaviour,
     * such as add customize headers or handle the response himself. <br/>
     * Note : before you override this, make sure you are understood the {@link FileDownloadRequest} very well.
     */
    public FileDownloadRequest buildRequest(DownloadItem item, FileDownloadRequest.FileDownloadListener listener) {
        return new FileDownloadRequest(item,listener);
    }

    public List<DownloadController> getAll() {
        synchronized (mTaskQueue) {
           return mTaskQueue;
        }

    }

    public class DownloadController {
        // Persist the Request createing params for re-create it when pause operation gone.
        FileDownloadRequest.FileDownloadListener mListener;

        // The download request.
        private FileDownloadRequest mRequest;

        private DownloadItem mDownloadItem;

        int mStatus;
        public static final int STATUS_WAITING = 0;
        public static final int STATUS_DOWNLOADING = 1;
        public static final int STATUS_PAUSE = 2;
        public static final int STATUS_SUCCESS = 3;
        public static final int STATUS_DISCARD = 4;

        DownloadController(DownloadItem item, FileDownloadRequest.FileDownloadListener listener) {
            mListener = listener;
            mDownloadItem = item;
        }

        /**
         * For the parallel reason, only the {@link FileDownloader#schedule()} can call this method.
         * @return true if deploy is successed.
         */
        boolean deploy() {
            if (mStatus != STATUS_WAITING) return false;

            FileDownloadRequest.FileDownloadListener fileDownloadListener =  new FileDownloadRequest.FileDownloadListener() {
                boolean isCanceled;

                @Override
                public void onSuccess(DownloadItem item) {
                    mStatus = STATUS_SUCCESS;
                    if (mListener != null && !isCanceled) {
                        mListener.onSuccess(item);
                    }
                    remove(DownloadController.this);
                }

                @Override
                public void onCancel(DownloadItem item) {
                    isCanceled = true;
                    if (mListener != null) {
                        mListener.onCancel(item);
                    }
                    remove(DownloadController.this);
                }


                @Override
                public void onErrorResponse(DownloadItem item) {
                    if (mListener != null && !isCanceled) {
                        mListener.onErrorResponse(item);
                    }
                    remove(DownloadController.this);
                }

                @Override
                public void downloadProgress(DownloadItem item, long totalSize, long readSize) {
                    if (mListener != null) {
                        mListener.downloadProgress(item,totalSize,readSize);
                    }
                }
            };
            mRequest = buildRequest(mDownloadItem,fileDownloadListener);

            mStatus = STATUS_DOWNLOADING;

            if(mRequestQueue == null){
                return false;
            }
            mRequestQueue.add(mRequest);
            return true;
        }

        public int getStatus() {
            return mStatus;
        }

        public boolean isDownloading() {
            return mStatus == STATUS_DOWNLOADING;
        }


        public String getUrl() {
            return mDownloadItem.url;
        }
        public String getStorePath() {
            return mDownloadItem.filePath;
        }
        public void setDownloadListener(FileDownloadRequest.FileDownloadListener listener) {
            mListener = listener;
        }

        public FileDownloadRequest.FileDownloadListener getDownloadListener() {
            return mListener;
        }

        /**
         * Pause this task when it status was DOWNLOADING, in fact, we just marked the request should be cancel,
         * http request cannot stop immediately, we assume it will finish soon, thus we set the status as PAUSE,
         * let Task Queue deploy a new Request, that will cause parallel tasks growing beyond maximum task count,
         * but it doesn't matter, we believe that situation never longer.
         * @return true if did the pause operation.
         */
        public boolean pause() {
            if (mStatus == STATUS_DOWNLOADING) {
                mStatus = STATUS_PAUSE;
                mRequest.cancel();
                schedule();
                return true;
            }
            return false;
        }

        public DownloadItem getDownloadItem() {
            return mDownloadItem;
        }

        /**
         * Resume this task when it status was PAUSE, we will turn the status as WAITING, then re-schedule the Task Queue,
         * if parallel counter take an idle place, this task will re-deploy instantly,
         * if not, the status will stay WAITING till idle occur.
         * @return true if did the resume operation.
         */
        public boolean resume() {
            if (mStatus == STATUS_PAUSE) {
                mStatus = STATUS_WAITING;
                schedule();
                return true;
            }
            return false;
        }

        /**
         * We will discard this task from the Task Queue, if the status was DOWNLOADING,
         * we first cancel the Request, then remove task from the Task Queue,
         * also re-schedule the Task Queue at last.
         * @return true if did the discard operation.
         */
        public boolean discard() {
            if (mStatus == STATUS_WAITING) {
                mStatus = STATUS_DISCARD;
                remove(this);

                if (mListener != null) {
                    mListener.onCancel(mDownloadItem);
                }
                return true;
            }

            if (mStatus == STATUS_DISCARD) return false;
            if (mStatus == STATUS_SUCCESS) return false;
            if (mStatus == STATUS_DOWNLOADING) mRequest.cancel();
            mStatus = STATUS_DISCARD;
            remove(this);
            return true;
        }
    }

}
