package com.czhj.sdk.common.utils;

import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;

public class AdvertisingId implements Serializable {

    private static final long ROTATION_TIME_MS = 24 * 60 * 60 * 1000;

    /**
     * time when Sigmob generated ID was rotated last time
     */

    final Calendar mLastRotation;

    /**
     * Advertising ID from device, may not always be available.
     * Empty string if ifa is not available.
     */

    public final String mAdvertisingId;

    /**
     * virtual device ID, rotated every 24 hours
     */

    final String mSigmobId;

    /**
     * limit ad tracking device setting
     */
    public final boolean mDoNotTrack;

    AdvertisingId(String ifaId,
                  String SigmobId,
                  boolean limitAdTrackingEnabled,
                  long rotationTime) {
        mAdvertisingId = ifaId;
        mSigmobId = SigmobId;
        mDoNotTrack = limitAdTrackingEnabled;
        mLastRotation = Calendar.getInstance();
        mLastRotation.setTimeInMillis(rotationTime);
    }


    public static AdvertisingId generateExpiredAdvertisingId() {
        Calendar time = Calendar.getInstance();
        String SigmobId = generateIdString();
        return new AdvertisingId(null, SigmobId, false, time.getTimeInMillis() - ROTATION_TIME_MS - 1);
    }


    static String generateIdString() {
        return UUID.randomUUID().toString();
    }

    boolean isRotationRequired() {
        Calendar now = Calendar.getInstance();
        return now.getTimeInMillis() - mLastRotation.getTimeInMillis() >= ROTATION_TIME_MS;
    }

    @Override
    public String toString() {
        return "AdvertisingId{" +
                "mLastRotation=" + mLastRotation +
                ", mAdvertisingId='" + mAdvertisingId + '\'' +
                ", mSigmobId='" + mSigmobId + '\'' +
                ", mDoNotTrack=" + mDoNotTrack +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof AdvertisingId)) return false;

        AdvertisingId that = (AdvertisingId) o;

        return mDoNotTrack == that.mDoNotTrack && mAdvertisingId.equals(that.mAdvertisingId) && mSigmobId.equals(that.mSigmobId);
    }

    @Override
    public int hashCode() {
        int result = mAdvertisingId.hashCode();
        result = 31 * result + mSigmobId.hashCode();
        result = 31 * result + (mDoNotTrack ? 1 : 0);
        return result;
    }
}

