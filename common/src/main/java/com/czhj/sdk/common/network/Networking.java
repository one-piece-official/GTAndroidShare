package com.czhj.sdk.common.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.czhj.sdk.common.Constants;
import com.czhj.sdk.common.ThreadPool.ThreadPoolFactory;
import com.czhj.sdk.common.utils.DeviceUtils;
import com.czhj.sdk.common.utils.Preconditions;
import com.czhj.sdk.common.utils.SharedPreferencesUtil;
import com.czhj.sdk.logger.SigmobLog;
import com.czhj.volley.Cache;
import com.czhj.volley.ExecutorDelivery;
import com.czhj.volley.Network;
import com.czhj.volley.toolbox.BaseHttpStack;
import com.czhj.volley.toolbox.BasicNetwork;
import com.czhj.volley.toolbox.FileDownloadNetwork;
import com.czhj.volley.toolbox.HurlStack;
import com.czhj.volley.toolbox.ImageLoader;
import com.czhj.volley.toolbox.NoCache;

import java.util.HashSet;

import javax.net.ssl.SSLSocketFactory;


public class Networking {

    private static final String CACHE_DIRECTORY_NAME = "sigmob-volley-cache";
    private static final String DEFAULT_USER_AGENT = System.getProperty("http.agent");

    // These are volatile so that double-checked locking works.
    // See https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
    // for more information.
    private volatile static SigmobRequestQueue sSigRequestQueue;
    private volatile static SigmobRequestQueue sFileDownloadRequestQueue;
    private volatile static SigmobRequestQueue sBuriedPointRequestQueue;
    private volatile static SigmobRequestQueue sCommonRequestQueue;

    private volatile static String sUserAgent;
    private volatile static String sCacheUserAgent = DEFAULT_USER_AGENT;
    private volatile static MaxWidthImageLoader sMaxWidthImageLoader;

    private static boolean sUseHttps = false;
    private static HurlStack.UrlRewriter sUrlRewriter;
    private volatile static Cache mCache = null;

    private static HashSet mServerURLS = new HashSet<>();
    private static SigmobRequestQueue sRequestQueue;
    private static SigmobRequestQueue mAdTrackerQueue;

    public static SigmobRequestQueue getSigRequestQueue() {
        return sSigRequestQueue;
    }

    public static SigmobRequestQueue getRequestQueue() {
        return sRequestQueue;
    }


    public static String getUserAgent() {
        if (TextUtils.isEmpty(sUserAgent)) {
            return sCacheUserAgent;
        }
        return sUserAgent;

    }

    public static SigmobRequestQueue getAdTrackerRetryQueue() {
        return mAdTrackerQueue;
    }

    public static HurlStack.UrlRewriter getUrlRewriter() {
        return sUrlRewriter;
    }

    public static SigmobRequestQueue getDownloadRequestQueue() {
        return sFileDownloadRequestQueue;
    }


    public static SigmobRequestQueue getBuriedPointRequestQueue() {
        return sBuriedPointRequestQueue;
    }


    private static Cache initializeCache(final Context context) {
        if (mCache == null) {

            mCache = new NoCache();
        }
        return mCache;
    }


    public static SigmobRequestQueue getCommonRequestQueue() {
        return sCommonRequestQueue;
    }

    private static HurlStack.UrlRewriter initializeUrlRewriter(final Context context) {

        // No synchronization done here since it's fine to create the same rewriter more than once.
        if (sUrlRewriter == null) {
            sUrlRewriter = new PlayServicesUrlRewriter();
        }
        return sUrlRewriter;
    }

    public static ImageLoader initializeImageLoader(Context context) {
        MaxWidthImageLoader imageLoader = sMaxWidthImageLoader;
        // Double-check locking to initialize.
        if (imageLoader == null) {
            synchronized (Networking.class) {
                imageLoader = sMaxWidthImageLoader;
                if (imageLoader == null) {
                    SigmobRequestQueue queue = getDownloadRequestQueue();
                    int cacheSize = DeviceUtils.memoryCacheSizeBytes(context);
                    final LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(cacheSize) {
                        @Override
                        protected int sizeOf(String key, Bitmap value) {
                            if (value != null) {
                                return value.getRowBytes() * value.getHeight();
                            }

                            return super.sizeOf(key, null);
                        }
                    };
                    imageLoader = new MaxWidthImageLoader(queue, context, new MaxWidthImageLoader.ImageCache() {
                        @Override
                        public Bitmap getBitmap(final String key) {
                            return imageCache.get(key);
                        }

                        @Override
                        public void putBitmap(final String key, final Bitmap bitmap) {
                            imageCache.put(key, bitmap);
                        }
                    });
                    sMaxWidthImageLoader = imageLoader;
                }
            }
        }
        return imageLoader;
    }

    private static SigmobRequestQueue initializeRequestQueue(final Context context) {
        SigmobRequestQueue requestQueue = sRequestQueue;
        // Double-check locking to initialize.
        if (requestQueue == null) {
            synchronized (Networking.class) {
                requestQueue = sRequestQueue;
                if (requestQueue == null) {

                    SSLSocketFactory socketFactory;
                    socketFactory = CustomSSLSocketFactory.getDefault(Constants.TEN_SECONDS_MILLIS);


                    final BaseHttpStack httpStack = new RequestQueueHttpStack(socketFactory);

                    final Network network = new BasicNetwork(httpStack);

                    requestQueue = new SigmobRequestQueue(network, 2, Integer.MAX_VALUE, new ExecutorDelivery(new Handler(Looper.getMainLooper())));
                    sRequestQueue = requestQueue;
                    requestQueue.start();
                }
            }
        }

        return requestQueue;
    }

    public static SigmobRequestQueue initializeSigRequestQueue(final Context context) {
        SigmobRequestQueue requestQueue = sSigRequestQueue;
        // Double-check locking to initialize.
        if (requestQueue == null) {
            synchronized (Networking.class) {
                requestQueue = sSigRequestQueue;
                if (requestQueue == null) {

                    SSLSocketFactory socketFactory;
                    socketFactory = CustomSSLSocketFactory.getDefault(Constants.TEN_SECONDS_MILLIS);


                    final BaseHttpStack httpStack = new RequestQueueHttpStack(socketFactory);

                    final Network network = new BasicNetwork(httpStack);

                    requestQueue = new SigmobRequestQueue(network, 2, 5);
                    sSigRequestQueue = requestQueue;
                    requestQueue.start();
                }
            }
        }

        return requestQueue;
    }

    public static SigmobRequestQueue initializeadTrackerReTryQueue(final Context context) {
        SigmobRequestQueue requestQueue = mAdTrackerQueue;
        // Double-check locking to initialize.
        if (requestQueue == null) {
            synchronized (Networking.class) {
                requestQueue = mAdTrackerQueue;
                if (requestQueue == null) {

                    SSLSocketFactory socketFactory;
                    socketFactory = CustomSSLSocketFactory.getDefault(Constants.TEN_SECONDS_MILLIS);

                    final BaseHttpStack httpStack = new RequestQueueHttpStack(socketFactory);

                    final Network network = new BasicNetwork(httpStack);

                    if (mCache == null) {
                        initializeCache(context);
                    }
                    requestQueue = new SigmobRequestQueue(network, 1, 10);
                    mAdTrackerQueue = requestQueue;
                    requestQueue.start();
                }
            }
        }

        return requestQueue;
    }


    public static void initializeUserAgentCache(final Context context) {
        sCacheUserAgent = SharedPreferencesUtil.getSharedPreferences(context).getString("user-agent-cache", DEFAULT_USER_AGENT);
    }

    public static void initialize(final Context context) {

        initializeUserAgentCache(context);
        initializeUserAgent(context);
        initializeBuriedPointRequestQueue(context);
        initializeDownloadRequestQueue(context);
        initializeRequestQueue(context);
        initializeCommonRequestQueue(context);
        initializeImageLoader(context);
        initializeadTrackerReTryQueue(context);
    }

    public static void initializeV2(final Context context) {

        initializeUserAgentCache(context);
        initializeUserAgent(context);
        initializeBuriedPointRequestQueue(context);
        initializeDownloadRequestQueue(context);
        initializeCommonRequestQueue(context);
        initializeadTrackerReTryQueue(context);
    }

    public static void initializeMill(final Context context) {

        initializeUserAgentCache(context);
        initializeUserAgent(context.getApplicationContext());
        initializeRequestQueue(context);
        initializeCommonRequestQueue(context);
        initializeBuriedPointRequestQueue(context);
    }


    public static SigmobRequestQueue initializeCommonRequestQueue(final Context context) {
        SigmobRequestQueue requestQueue = sCommonRequestQueue;
        // Double-check locking to initialize.
        if (requestQueue == null) {
            synchronized (Networking.class) {
                requestQueue = sCommonRequestQueue;
                if (requestQueue == null) {

                    SSLSocketFactory socketFactory;
                    socketFactory = CustomSSLSocketFactory.getDefault(Constants.TEN_SECONDS_MILLIS);


                    final BaseHttpStack httpStack = new RequestQueueHttpStack(socketFactory);

                    final Network network = new BasicNetwork(httpStack);

                    requestQueue = new SigmobRequestQueue(network, 2, 10);
                    sCommonRequestQueue = requestQueue;
                    requestQueue.start();
                }
            }
        }

        return requestQueue;
    }

    public static SigmobRequestQueue initializeBuriedPointRequestQueue(final Context context) {
        SigmobRequestQueue requestQueue = sBuriedPointRequestQueue;
        // Double-check locking to initialize.
        if (requestQueue == null) {
            synchronized (Networking.class) {
                requestQueue = sBuriedPointRequestQueue;
                if (requestQueue == null) {

                    SSLSocketFactory socketFactory;
                    socketFactory = CustomSSLSocketFactory.getDefault(Constants.TEN_SECONDS_MILLIS);


                    final BaseHttpStack httpStack = new RequestQueueHttpStack(socketFactory);

                    final Network network = new BasicNetwork(httpStack);

                    requestQueue = new SigmobRequestQueue(network, 1, 2);
                    sBuriedPointRequestQueue = requestQueue;
                    requestQueue.start();
                }
            }
        }

        return requestQueue;
    }


    public static SigmobRequestQueue initializeDownloadRequestQueue(final Context context) {
        SigmobRequestQueue requestQueue = sFileDownloadRequestQueue;
        // Double-check locking to initialize.
        if (requestQueue == null) {
            synchronized (Networking.class) {
                requestQueue = sFileDownloadRequestQueue;
                if (requestQueue == null) {


                    SSLSocketFactory socketFactory;
                    socketFactory = CustomSSLSocketFactory.getDefault(Constants.TEN_SECONDS_MILLIS);

                    final BaseHttpStack httpStack = new RequestQueueHttpStack(socketFactory);

                    final Network network = new FileDownloadNetwork(httpStack);


                    requestQueue = new SigmobRequestQueue(network, 1, 6);
                    sFileDownloadRequestQueue = requestQueue;
                    requestQueue.start();
                }
            }
        }

        return requestQueue;
    }


    /**
     * Caches and returns the WebView user agent to be used across all SDK requests. This is
     * important because advertisers expect the same user agent across all request, impression, and
     * click events.
     */

    public static String initializeUserAgent(final Context context) {
        Preconditions.NoThrow.checkNotNull(context);

        String userAgent = sUserAgent;
        if (userAgent == null) {
            synchronized (Networking.class) {
                userAgent = sUserAgent;
                if (userAgent == null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            ThreadPoolFactory.BackgroundThreadPool.getInstance().submit(new Runnable() {
                                @Override
                                public void run() {
                                    long start = System.currentTimeMillis();
                                    sUserAgent = WebSettings.getDefaultUserAgent(context);
                                    SigmobLog.d("getUA time " + (System.currentTimeMillis() - start));
                                    SharedPreferences.Editor edit = SharedPreferencesUtil.getSharedPreferences(context).edit();
                                    edit.putString("user-agent-cache", sUserAgent);
                                    edit.apply();
                                }
                            });

                        } else {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    try {

                                        if (Looper.myLooper() == Looper.getMainLooper()) {
                                            // WebViews may only be instantiated on the UI thread. If anything goes
                                            // wrong with getting a user agent, use the system-specific user agent.
                                            WebView webView = new WebView(context);
                                            webView.removeJavascriptInterface("searchBoxJavaBridge_");
                                            webView.removeJavascriptInterface("accessibility");
                                            webView.removeJavascriptInterface("accessibilityTraversal");
                                            sUserAgent = webView.getSettings().getUserAgentString();
                                            SharedPreferences.Editor edit = SharedPreferencesUtil.getSharedPreferences(context).edit();
                                            edit.putString("user-agent-cache", sUserAgent);
                                            edit.apply();
                                        }
                                    } catch (Throwable e) {
                                        // Some custom ROMs may fail to get a user agent. If that happens, return
                                        // the Android system user agent.
                                    }
                                }
                            });

                        }
                    } catch (Throwable e) {
                        // Some custom ROMs may fail to get a user agent. If that happens, return
                        // the Android system user agent.
                    }


                }
            }
        }

        return userAgent;
    }

    /**
     * Gets the previously cached WebView user agent. This returns the default userAgent if the
     * WebView user agent has not been initialized yet.
     *
     * @return Best-effort String WebView user agent.
     */

    public static String getCachedUserAgent() {
        final String userAgent = sUserAgent;
        if (userAgent == null) {
            return DEFAULT_USER_AGENT;
        }
        return userAgent;
    }

    /**
     * Set whether to use HTTP or HTTPS for WebView base urls.
     */
    public static void useHttps(boolean useHttps) {
        sUseHttps = useHttps;
    }

    private static boolean shouldUseHttps() {
        return sUseHttps;
    }

    /**
     * Retrieve the scheme that should be used to communicate to the ad server. This should always
     * return https.
     *
     * @return "https"
     */
    public static String getScheme() {
        return Constants.HTTPS;
    }

    /**
     * DSPs are currently not ready for full https creatives. When we flip the switch to go full
     * https, this should just return https. However, for now, we allow the publisher to use
     * either http or https. This only affects WebView base urls.
     *
     * @return "https" if {@link #shouldUseHttps()} is true; "http" otherwise.
     */
    public static String getBaseUrlScheme() {
        return shouldUseHttps() ? Constants.HTTPS : Constants.HTTP;
    }


    public static void AddSigmobServerURL(String url) {
        mServerURLS.add(url);
    }

    public static HashSet getSigmobServerURLS() {
        return mServerURLS;
    }
}
