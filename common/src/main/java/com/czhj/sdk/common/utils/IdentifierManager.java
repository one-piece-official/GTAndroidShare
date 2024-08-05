package com.czhj.sdk.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.czhj.sdk.common.Constants;
import com.czhj.sdk.logger.SigmobLog;

import java.util.Calendar;

public class IdentifierManager {
    private long mGAIDLimit;

    public interface AdvertisingIdChangeListener {
        void onIdChanged(final AdvertisingId oldId, final AdvertisingId newId);
    }

    private static final String PREF_AD_INFO_GROUP = "com.Sigmob.settings.identifier";
    private static final String PREF_IFA_IDENTIFIER = "privacy.identifier.ifa";
    private static final String PREF_IFA_IDENTIFIER_AES = "privacy.identifier.ifa_aes_gcm";

    private static final String PREF_Sigmob_IDENTIFIER = "privacy.identifier.Sigmob";
    private static final String PREF_IDENTIFIER_TIME = "privacy.identifier.time";
    private static final String PREF_LIMIT_AD_TRACKING = "privacy.limit.ad.tracking";
    private static final int MISSING_VALUE = -1;

    interface SdkInitializationListener {
        void onInitializationFinished();
    }


    private AdvertisingId mAdInfo;

    private Context mAppContext;

    private AdvertisingIdChangeListener mIdChangeListener;

    private boolean mRefreshingAdvertisingInfo;

    private boolean initialized;

    private SdkInitializationListener mInitializationListener;


    public IdentifierManager(final Context appContext, final AdvertisingIdChangeListener idChangeListener) {

        if (Constants.GOOGLE_PLAY) {
            Preconditions.NoThrow.checkNotNull(appContext);

            mAppContext = appContext;
            mIdChangeListener = idChangeListener;
            mAdInfo = readIdFromStorage(mAppContext);
            if (mAdInfo == null) {
                mAdInfo = AdvertisingId.generateExpiredAdvertisingId();
            }
            refreshAdvertisingInfo();
        }

    }

    /**
     * @return the most recent advertising ID and Do Not Track settings. This method  internally
     * initiates AdvertisingId refresh. The value is returned instantly on UI thread,
     * but may take some time to communicate with Google Play Services API when called
     * from background thread.
     */

    public AdvertisingId getAdvertisingInfo() {
        final AdvertisingId adInfo = mAdInfo;


        mGAIDLimit = System.currentTimeMillis();
//        refreshAdvertisingInfo();
        return adInfo;
    }

    private void refreshAdvertisingInfo() {
        if (mRefreshingAdvertisingInfo) {
            return;
        }
        mRefreshingAdvertisingInfo = true;
        new RefreshAdvertisingInfoAsyncTask().execute();
    }

    private void refreshAdvertisingInfoBackgroundThread() {
        long time = Calendar.getInstance().getTimeInMillis();

        // try google
        if (isPlayServicesAvailable()) {

            PlayServicesUtil.AdvertisingInfo info = null;
            try {
                info = PlayServicesUtil.getAdvertisingIdInfo(mAppContext);
            } catch (Throwable e) {
                info = null;
            }
            if (info != null) {
                final AdvertisingId oldId = mAdInfo;
                if (info.limitAdTracking && oldId.isRotationRequired()) {
                    setAdvertisingInfo(info.advertisingId, AdvertisingId.generateIdString(), info.limitAdTracking, time);
                } else {
                    setAdvertisingInfo(info.advertisingId, oldId.mSigmobId, info.limitAdTracking, oldId.mLastRotation.getTimeInMillis());
                }
            }
        }
    }


    private static synchronized AdvertisingId readIdFromStorage(final Context appContext) {
        Preconditions.NoThrow.checkNotNull(appContext);

        Calendar now = Calendar.getInstance();
        try {
            final SharedPreferences preferences = SharedPreferencesUtil.getSharedPreferences(appContext, PREF_AD_INFO_GROUP);

            final String ifa_id_aes = preferences.getString(PREF_IFA_IDENTIFIER_AES, "");
            final String ifa_id;
            if (!TextUtils.isEmpty(ifa_id_aes)) {
                ifa_id = AESUtil.DecryptString(ifa_id_aes, Constants.AES_KEY);
            } else {
                ifa_id = preferences.getString(PREF_IFA_IDENTIFIER, "");
            }

            final String Sigmob_id = preferences.getString(PREF_Sigmob_IDENTIFIER, "");
            final long time = preferences.getLong(PREF_IDENTIFIER_TIME, now.getTimeInMillis());
            final boolean limitTracking = preferences.getBoolean(PREF_LIMIT_AD_TRACKING, false);
            if (!TextUtils.isEmpty(ifa_id) && !TextUtils.isEmpty(Sigmob_id)) {
                return new AdvertisingId(ifa_id, Sigmob_id, limitTracking, time);
            }
        } catch (Throwable ex) {
            SigmobLog.e("Cannot read identifier from shared preferences");
        }
        return null;
    }

    private static synchronized void writeIdToStorage(final Context context, final AdvertisingId info) {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(info);

        final SharedPreferences preferences = SharedPreferencesUtil.getSharedPreferences(context, PREF_AD_INFO_GROUP);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_LIMIT_AD_TRACKING, info.mDoNotTrack);
        editor.remove(PREF_IFA_IDENTIFIER);
        editor.putString(PREF_IFA_IDENTIFIER_AES, AESUtil.EncryptString(info.mAdvertisingId, Constants.AES_KEY));
        editor.putString(PREF_Sigmob_IDENTIFIER, info.mSigmobId);
        editor.putLong(PREF_IDENTIFIER_TIME, info.mLastRotation.getTimeInMillis());
        editor.apply();
    }


    private void setAdvertisingInfo(String advertisingId, String SigmobId, boolean limitAdTracking, long rotationTime) {
        Preconditions.NoThrow.checkNotNull(advertisingId);
        Preconditions.NoThrow.checkNotNull(SigmobId);

        setAdvertisingInfo(new AdvertisingId(advertisingId, SigmobId, limitAdTracking, rotationTime));
    }

    private void setAdvertisingInfo(final AdvertisingId newId) {
        AdvertisingId oldId = mAdInfo;
        mAdInfo = newId;
        writeIdToStorage(mAppContext, mAdInfo);

        if (!mAdInfo.equals(oldId) || !initialized) {
            notifyIdChangeListener(oldId, mAdInfo);
        }

        if (!initialized) {
            reportInitializationComplete();
        }
    }

    private void reportInitializationComplete() {
        if (mInitializationListener != null) {
            mInitializationListener.onInitializationFinished();
            mInitializationListener = null;
        }
        initialized = true;
    }

    private void notifyIdChangeListener(final AdvertisingId oldId, final AdvertisingId newId) {
        Preconditions.NoThrow.checkNotNull(newId);

        if (mIdChangeListener != null) {
            mIdChangeListener.onIdChanged(oldId, newId);
        }
    }

    private boolean isPlayServicesAvailable() {
        return true;
    }

    private class RefreshAdvertisingInfoAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(final Void... voids) {
            refreshAdvertisingInfoBackgroundThread();
            mRefreshingAdvertisingInfo = false;
            return null;
        }
    }
}
