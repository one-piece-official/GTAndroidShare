package com.czhj.volley;

import java.util.concurrent.ExecutorService;

public class ExecutorsDelivery implements ResponseDelivery {
    /**
     * Used for posting responses, typically to the main thread.
     */
    private final ExecutorService mResponsePoster;

    /**
     * Creates a new response delivery interface.
     *
     * @param ExecutorService {@link ExecutorService} to post responses on
     */
    public ExecutorsDelivery(final ExecutorService executorService) {
        // Make an Executor that just wraps the handler.
        mResponsePoster = executorService;
    }


    @Override
    public void postResponse(Request<?> request, Response<?> response) {
        postResponse(request, response, null);
    }

    @Override
    public void postResponse(Request<?> request, Response<?> response, Runnable runnable) {
        request.markDelivered();
        request.addMarker("post-response");
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, runnable));
    }

    @Override
    public void postError(Request<?> request, VolleyError error) {
        request.addMarker("post-error");
        Response<?> response = Response.error(error);
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, null));
    }

}
