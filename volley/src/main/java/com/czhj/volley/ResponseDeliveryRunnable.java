package com.czhj.volley;

/**
 * A Runnable used for delivering network responses to a listener on the main thread.
 */
@SuppressWarnings("rawtypes")
class ResponseDeliveryRunnable implements Runnable {
    private final Request mRequest;
    private final Response mResponse;
    private final Runnable mRunnable;

    public ResponseDeliveryRunnable(Request request, Response response, Runnable runnable) {
        mRequest = request;
        mResponse = response;
        mRunnable = runnable;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        // NOTE: If cancel() is called off the thread that we're currently running in (by
        // default, the main thread), we cannot guarantee that deliverResponse()/deliverError()
        // won't be called, since it may be canceled after we check isCanceled() but before we
        // deliver the response. Apps concerned about this guarantee must either call cancel()
        // from the same thread or implement their own guarantee about not invoking their
        // listener after cancel() has been called.

        // If this request has canceled, finish it and don't deliver.
        if (mRequest.isCanceled()) {
            mRequest.finish("canceled-at-delivery");
            return;
        }

        // Deliver a normal response or error, depending.
        if (mResponse.isSuccess()) {
            mRequest.deliverResponse(mResponse.result);
        } else {
            mRequest.deliverError(mResponse.error);
        }

        // If this is an intermediate response, add a marker, otherwise we're done
        // and the request can be finished.
        if (mResponse.intermediate) {
            mRequest.addMarker("intermediate-response");
        } else {
            mRequest.finish("done");
        }

        // If we have been provided a post-delivery runnable, run it.
        if (mRunnable != null) {
            mRunnable.run();
        }
    }
}
