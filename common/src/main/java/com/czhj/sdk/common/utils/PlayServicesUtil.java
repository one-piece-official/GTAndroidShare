package com.czhj.sdk.common.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;

import com.czhj.sdk.common.Constants;
import com.czhj.sdk.logger.SigmobLog;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class PlayServicesUtil {

    public static class AdvertisingInfo {
        public final boolean limitAdTracking;
        public final String advertisingId;

        AdvertisingInfo(String advertisingId, boolean isLimitAdTrackingEnabled) {
            this.advertisingId = advertisingId;
            this.limitAdTracking = isLimitAdTrackingEnabled;
        }
    }

    public static AdvertisingInfo getAdvertisingIdInfo(Context context) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper() || !Constants.GOOGLE_PLAY) {
            return null;
        }
        SigmobLog.d("private : getAdvertisingIdInfo");

        AdvertisingConnection connection = new AdvertisingConnection();
        Intent intent = new Intent(
                "com.google.android.gms.ads.identifier.service.START");
        intent.setPackage("com.google.android.gms");
        try {
            if (context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {

                AdvertisingInterface adInterface = new AdvertisingInterface(
                        connection.getBinder());
                return new AdvertisingInfo(adInterface.getId(),
                        adInterface.isLimitAdTrackingEnabled());
            }
        } finally {
            context.unbindService(connection);
        }

        throw new IOException("Google Play connection failed");
    }

    private static final class AdvertisingConnection implements
            ServiceConnection {
        boolean retrieved = false;
        private final LinkedBlockingQueue<IBinder> queue = new LinkedBlockingQueue<>(
                1);

        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                this.queue.put(service);
            } catch (Throwable th) {
                SigmobLog.e(th.getMessage());
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }

        IBinder getBinder() throws InterruptedException {
            if (this.retrieved)
                throw new IllegalStateException();
            this.retrieved = true;
            return this.queue.take();
        }
    }

    private static final class AdvertisingInterface implements IInterface {
        private final IBinder binder;

        AdvertisingInterface(IBinder pBinder) {
            binder = pBinder;
        }

        public IBinder asBinder() {
            return binder;
        }

        String getId() throws RemoteException {

            if (Constants.GOOGLE_PLAY) {

                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                String id;
                try {
                    data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                    binder.transact(1, data, reply, 0);
                    reply.readException();
                    id = reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return id;
            }
            return null;

        }

        boolean isLimitAdTrackingEnabled()
                throws RemoteException {
            if (Constants.GOOGLE_PLAY) {

                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                boolean limitAdTracking;
                try {
                    data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                    data.writeInt(1);
                    binder.transact(2, data, reply, 0);
                    reply.readException();
                    limitAdTracking = 0 != reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return limitAdTracking;
            }
            return false;
        }
    }

}
