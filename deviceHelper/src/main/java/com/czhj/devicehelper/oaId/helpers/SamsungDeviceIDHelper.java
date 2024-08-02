package com.czhj.devicehelper.oaId.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.util.Log;


import com.czhj.devicehelper.oaId.interfaces.SamsungIDInterface;
import com.czhj.sdk.logger.SigmobLog;

import java.util.concurrent.LinkedBlockingQueue;

/****************************
 * * on 2019/10/29
 ****************************
 */
public class SamsungDeviceIDHelper {

  private Context mContext;
  public final LinkedBlockingQueue<IBinder> linkedBlockingQueue = new LinkedBlockingQueue(1);

  public SamsungDeviceIDHelper(Context ctx) {
    mContext = ctx;
  }

  public void getSumsungID(DevicesIDsHelper.AppIdsUpdater _listener) {

    if (!isSupport()){
      return;
    }
    Intent intent = new Intent();
    intent.setClassName("com.samsung.android.deviceidservice", "com.samsung.android.deviceidservice.DeviceIdService");
    boolean isBinded = mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    if (isBinded) {
      try {
        IBinder iBinder = linkedBlockingQueue.take();
        SamsungIDInterface.Proxy proxy = new SamsungIDInterface.Proxy(iBinder);       // 在这里有区别，需要实际验证
        String oaid = proxy.getID();
        if (_listener != null) {
          _listener.OnIdsAvalid(oaid);
        }
      }
      catch (Exception e) {
        SigmobLog.e(e.getMessage());
      }finally{
        mContext.unbindService(serviceConnection);
      }
    }

  }

  ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.d("oaid", "oaid onServiceConnected() called with: name = [" + name + "], service = [" + service + "]");
      try {
        linkedBlockingQueue.put(service);
      }
      catch (Exception e) {
         SigmobLog.e(e.getMessage());
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
  };
    public boolean isSupport() {
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo("com.samsung.android.deviceidservice", 0);
            return pi != null;
        } catch (Throwable e) {
            return false;
        }
    }
}
