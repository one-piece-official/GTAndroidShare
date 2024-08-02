package com.czhj.devicehelper.oaId.helpers;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import com.czhj.devicehelper.oaId.interfaces.OppoIDInterface;
import com.czhj.sdk.logger.SigmobLog;

import java.security.MessageDigest;
import java.util.concurrent.LinkedBlockingQueue;

/****************************
 * on 2019/10/29
 ****************************
 */
public class OppoDeviceIDHelper {

    private Context mContext;
    public String oaid = "OUID";
    private String sign;
    OppoIDInterface oppoIDInterface;
    public final LinkedBlockingQueue<IBinder> linkedBlockingQueue = new LinkedBlockingQueue(1);

    public OppoDeviceIDHelper(Context ctx) {
        mContext = ctx;
    }

    public void getID(DevicesIDsHelper.AppIdsUpdater _listener) {

        if (!isSupport()) {
            return;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return;
        }

        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.heytap.openid", "com.heytap.openid.IdentifyService"));
        intent.setAction("action.com.heytap.openid.OPEN_ID_SERVICE");

        if (mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
            try {
                IBinder iBinder = linkedBlockingQueue.take();
                oppoIDInterface = OppoIDInterface.up.genInterface(iBinder);
                if (oppoIDInterface != null) {
                    String oaid = realoGetIds("OUID");
                    if (_listener != null) {
                        _listener.OnIdsAvalid(oaid);
                    }
                }
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            } finally {
                mContext.unbindService(serviceConnection);
            }

        }
    }

    @SuppressLint("WrongConstant")
    private String realoGetIds(String str) {
        String res = null;

        String str2 = null;
        String pkgName = mContext.getPackageName();
        if (sign == null) {
            Signature[] signatures;
            try {
                signatures = mContext.getPackageManager().getPackageInfo(pkgName, 64).signatures;
            } catch (Exception e) {

                SigmobLog.e(e.getMessage());
                signatures = null;
            }

            if (signatures != null && signatures.length > 0) {
                byte[] byteArray = signatures[0].toByteArray();
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                    if (messageDigest != null) {
                        byte[] digest = messageDigest.digest(byteArray);
                        StringBuilder sb = new StringBuilder();
                        for (byte b : digest) {
                            sb.append(Integer.toHexString((b & 255) | 256).substring(1, 3));
                        }
                        str2 = sb.toString();
                    }
                } catch (Exception e) {

                    SigmobLog.e(e.getMessage());
                }
            }
            sign = str2;

        }

        res = ((OppoIDInterface.up.down) oppoIDInterface).getSerID(pkgName, sign, str);
        return res;
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                linkedBlockingQueue.put(service);
            } catch (Throwable t) {

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            oppoIDInterface = null;
        }
    };

    private boolean isSupport() {
        boolean res = false;

        try {

            PackageManager pm = mContext.getPackageManager();
            String pNname = "com.heytap.openid";

            PackageInfo pi = pm.getPackageInfo(pNname, 0);
            if (pi == null) {
                return false;
            }
            long ver = 0;
            if (Build.VERSION.SDK_INT >= 28) {
                ver = pi.getLongVersionCode();
            } else {
                ver = pi.versionCode;
            }

            if (ver < 1) {
                return false;
            }
            return true;
        } catch (Exception e) {
            SigmobLog.e(e.getMessage());
        }
        return res;
    }
}
