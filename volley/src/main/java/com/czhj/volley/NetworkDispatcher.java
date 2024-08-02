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

import android.net.TrafficStats;
import android.os.SystemClock;

/**
 * Provides a thread for performing network dispatch from a queue of requests.
 * <p>
 * <p>Requests added to the specified queue are processed from the network via a specified {@link
 * Network} interface. Responses are committed to cache, if eligible, using a specified {@link
 * Cache} interface. Valid responses and errors are posted back to the caller via a {@link
 * ResponseDelivery}.
 */
public class NetworkDispatcher implements Runnable,Comparable {


    /**
     * The network interface for processing requests.
     */
    private final Network mNetwork;

    /**
     * For posting responses and errors.
     */
    private final ResponseDelivery mDelivery;
    private final Request mRequest;
    /**
     * Used for telling us to die.
     */
    private volatile boolean mQuit = false;

    /**
     * Creates a new network dispatcher Runable.
     * processing.

     * @param delivery Delivery interface to use for posting responses
     */
    public NetworkDispatcher(
            Network network,
            Request request,
            ResponseDelivery delivery) {
        mRequest = request;
        mNetwork = network;
        mDelivery = delivery;
    }

    /**
     * Forces this dispatcher to quit immediately. If any requests are still in the queue, they are
     * not guaranteed to be processed.
     */
    public void quit() {
        mQuit = true;
    }

    private void addTrafficStatsTag(Request<?> request) {
        // Tag the request (if API >= 14)
        TrafficStats.setThreadStatsTag(request.getTrafficStatsTag());
    }

    @Override
    public void run() {

        processRequest(mRequest);
    }


    public void processRequest(Request<?> request) {
        long startTimeMs = SystemClock.elapsedRealtime();
        try {
            request.addMarker("network-queue-take");

            // If the request was cancelled already, do not perform the
            // network request.
            if (request.isCanceled()) {
                request.finish("network-discard-cancelled");
                request.notifyListenerResponseNotUsable();
                return;
            }

            addTrafficStatsTag(request);

            // Perform the network request.
            NetworkResponse networkResponse = mNetwork.performRequest(request);
            request.addMarker("network-http-complete");

            // If the server returned 304 AND we delivered a response already,
            // we're done -- don't deliver a second identical response.
            if (networkResponse.notModified && request.hasHadResponseDelivered()) {
                request.finish("not-modified");
                request.notifyListenerResponseNotUsable();
                return;
            }

            // Parse the response here on the worker thread.
            Response<?> response = request.parseNetworkResponse(networkResponse);
            request.addMarker("network-parse-complete");


            // Post the response back.
            request.markDelivered();
            mDelivery.postResponse(request, response);
            request.notifyListenerResponseReceived(response);
        } catch (VolleyError volleyError) {
            volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
            parseAndDeliverNetworkError(request, volleyError);
            request.notifyListenerResponseNotUsable();
        } catch (Throwable e) {
            VolleyLog.e(e, "Unhandled exception %s", e.toString());
            VolleyError volleyError = new VolleyError(e);
            volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
            mDelivery.postError(request, volleyError);
            request.notifyListenerResponseNotUsable();
        }
    }

    private void parseAndDeliverNetworkError(Request<?> request, VolleyError error) {
        error = request.parseNetworkError(error);
        mDelivery.postError(request, error);
    }


    @Override
    public int compareTo(Object o) {
      return o != null?1:hashCode()!=o.hashCode()?1:0;
    }
}
