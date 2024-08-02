package com.czhj.devicehelper.oaId.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;

import com.czhj.devicehelper.oaId.interfaces.ASUSIDInterface;
import com.czhj.sdk.logger.SigmobLog;

import java.util.concurrent.LinkedBlockingQueue;

/****************************
 * on 2019/10/29
 * 华硕手机获取 OAID
 ****************************
 */
public class ASUSDeviceIDHelper {

    private Context mContext;
    public final LinkedBlockingQueue<IBinder> linkedBlockingQueue = new LinkedBlockingQueue(1);

    public ASUSDeviceIDHelper(Context ctx) {
        mContext = ctx;
    }

    /**
     * 获取 OAID 并回调
     *
     * @param _listener
     */
    public void getID(DevicesIDsHelper.AppIdsUpdater _listener) {

        if (!isSupport()) {
            return;
        }

        Intent intent = new Intent();
        intent.setAction("com.asus.msa.action.ACCESS_DID");
        ComponentName componentName = new ComponentName("com.asus.msa.SupplementaryDID", "com.asus.msa.SupplementaryDID.SupplementaryDIDService");
        intent.setComponent(componentName);

        boolean isBin = mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        if (isBin) {
            try {
                IBinder iBinder = linkedBlockingQueue.take();
                ASUSIDInterface.ASUSID asusID = new ASUSIDInterface.ASUSID(iBinder);
                String asusOAID = asusID.getID();

                if (_listener != null) {
                    _listener.OnIdsAvalid(asusOAID);
                }
            } catch (Exception e) {
                SigmobLog.e(e.getMessage());
            } finally {
                mContext.unbindService(serviceConnection);
            }
        }
    }

    public boolean isSupport() {
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo("com.asus.msa.SupplementaryDID", 0);
            return pi != null;
        } catch (Throwable e) {
            return false;
        }
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                linkedBlockingQueue.put(service);
            } catch (Exception e) {
                SigmobLog.e(e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
}
