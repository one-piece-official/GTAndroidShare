package com.czhj.sdk.common.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;

import com.czhj.sdk.common.ClientMetadata;
import com.czhj.sdk.logger.SigmobLog;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AdLifecycleManager {

    private final Set<WeakReference<LifecycleListener>> mLifecycleListeners;

    private static AdLifecycleManager gInstance;

    private boolean isInit;

    AdLifecycleManager() {
        mLifecycleListeners = new HashSet<>();
    }

    public void initialize(final Application application) {
        try {

            if (isInit) {
                return;
            }

            activityCallBack(application);

            isInit = true;

        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
    }


    public static AdLifecycleManager getInstance() {

        if (gInstance == null) {
            synchronized (AdLifecycleManager.class) {
                if (gInstance == null) {
                    gInstance = new AdLifecycleManager();
                }
            }
        }
        return gInstance;
    }

    private void activityCallBack(final Application application) {

        if (application == null) {
            SigmobLog.e("activityCallBack error, application is null");
            return;
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                SigmobLog.d("onActivityCreated() called with: activity = [" + activity + "], savedInstanceState = [" + savedInstanceState + "]");
                onCreate(activity);

            }

            @Override
            public void onActivityStarted(Activity activity) {
                SigmobLog.d("onActivityStarted() called with: activity = [" + activity + "]");
                onStart(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                SigmobLog.d("onActivityResumed() called with: activity = [" + activity + "]");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    WindowInsets windowInsets = activity.getWindow().getDecorView().getRootWindowInsets();

                    ClientMetadata.getInstance().setWindInsets(windowInsets);
                }

                onResume(activity);


            }

            @Override
            public void onActivityPaused(Activity activity) {
                SigmobLog.d("onActivityPaused() called with: activity = [" + activity + "]");
                onPause(activity);

            }

            @Override
            public void onActivityStopped(Activity activity) {
                SigmobLog.d("onActivityStopped() called with: activity = [" + activity + "]");
                onStop(activity);


            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                SigmobLog.d("onActivitySaveInstanceState() called with: activity = [" + activity + "], outState = [" + outState + "]");

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                SigmobLog.d("onActivityDestroyed() called with: activity = [" + activity + "]");
                onDestroy(activity);

            }
        });
    }


    public void addLifecycleListener(LifecycleListener listener) {
        // Get the instance or bail if not initialized.
        if (listener == null) {
            return;
        }

        if (mLifecycleListeners != null && !isContains(listener)) {
            mLifecycleListeners.add(new WeakReference<LifecycleListener>(listener));
        }
    }

    private synchronized boolean isContains(LifecycleListener listener) {
        WeakReference<LifecycleListener> lifecycleListenerWeakReference = getLifecycleListenerWeakReference(listener);
        if (lifecycleListenerWeakReference != null) {
            return true;
        }
        return false;
    }

    private synchronized WeakReference<LifecycleListener> getLifecycleListenerWeakReference(LifecycleListener listener) {
        try {

            CopyOnWriteArraySet<WeakReference<LifecycleListener>> tempList = new CopyOnWriteArraySet(mLifecycleListeners);
            for (WeakReference<LifecycleListener> weakReference : tempList) {
                if (weakReference.get() == listener) {
                    return weakReference;
                }
            }
        } catch (Throwable t) {

        }
        return null;
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        // Get the instance or bail if not initialized.
        if (listener == null) {
            return;
        }

        if (mLifecycleListeners != null) {
            WeakReference<LifecycleListener> lifecycleListenerWeakReference = getLifecycleListenerWeakReference(listener);
            if (lifecycleListenerWeakReference != null) {
                mLifecycleListeners.remove(lifecycleListenerWeakReference);
            }
        }
    }

    private synchronized void onCreate(final Activity activity) {

        try {
            CopyOnWriteArraySet<WeakReference<LifecycleListener>> tempList = new CopyOnWriteArraySet(mLifecycleListeners);
            for (WeakReference<LifecycleListener> weakReference : tempList) {
                LifecycleListener listener = weakReference.get();
                if (listener != null) {
                    listener.onCreate(activity);
                }
            }
        } catch (Throwable t) {
        }
    }

    private synchronized void onStart(final Activity activity) {
        try {
            CopyOnWriteArraySet<WeakReference<LifecycleListener>> tempList = new CopyOnWriteArraySet(mLifecycleListeners);
            for (WeakReference<LifecycleListener> weakReference : tempList) {
                LifecycleListener listener = weakReference.get();
                if (listener != null) {
                    listener.onStart(activity);
                }
            }
        } catch (Throwable t) {

        }


    }

    private synchronized void onPause(final Activity activity) {


        try {
            CopyOnWriteArraySet<WeakReference<LifecycleListener>> tempList = new CopyOnWriteArraySet(mLifecycleListeners);
            for (WeakReference<LifecycleListener> weakReference : tempList) {
                LifecycleListener listener = weakReference.get();
                if (listener != null) {
                    listener.onPause(activity);
                }
            }
        } catch (Throwable t) {

        }
    }

    private synchronized void onResume(final Activity activity) {

        try {
            CopyOnWriteArraySet<WeakReference<LifecycleListener>> tempList = new CopyOnWriteArraySet(mLifecycleListeners);
            for (WeakReference<LifecycleListener> weakReference : tempList) {
                LifecycleListener listener = weakReference.get();
                if (listener != null) {
                    listener.onResume(activity);
                }
            }
        } catch (Throwable t) {

        }
    }


    private synchronized void onStop(final Activity activity) {
        try {
            CopyOnWriteArraySet<WeakReference<LifecycleListener>> tempList = new CopyOnWriteArraySet(mLifecycleListeners);
            for (WeakReference<LifecycleListener> weakReference : tempList) {
                LifecycleListener listener = weakReference.get();
                if (listener != null) {
                    listener.onStop(activity);
                }
            }
        } catch (Throwable t) {

        }

    }

    private synchronized void onDestroy(final Activity activity) {

        try {
            CopyOnWriteArraySet<WeakReference<LifecycleListener>> tempList = new CopyOnWriteArraySet(mLifecycleListeners);
            for (WeakReference<LifecycleListener> weakReference : tempList) {
                LifecycleListener listener = weakReference.get();
                if (listener != null) {
                    listener.onDestroy(activity);
                }
            }
        } catch (Throwable t) {
        }

    }

    public interface LifecycleListener {
        void onCreate(Activity activity);

        void onStart(Activity activity);

        void onPause(Activity activity);

        void onResume(Activity activity);

        void onStop(Activity activity);

        void onDestroy(Activity activity);
    }
}
