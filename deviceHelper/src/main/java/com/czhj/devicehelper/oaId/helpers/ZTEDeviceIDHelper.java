package com.czhj.devicehelper.oaId.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.util.Log;


import com.czhj.devicehelper.oaId.interfaces.ZTEIDInterface;
import com.czhj.sdk.logger.SigmobLog;

import java.util.concurrent.LinkedBlockingQueue;


public class ZTEDeviceIDHelper {
  Context mContext;
  public ZTEDeviceIDHelper(Context ctx) {
    mContext = ctx;
  }

  private boolean checkService() {
    try {
      PackageInfo pi = mContext.getPackageManager().getPackageInfo("com.mdid.msa", 0);
      return pi != null;
    } catch (Exception e) {
       SigmobLog.e(e.getMessage());
    }
    return false;
  }

  private void startMsaklServer(String pkgName) {

    try {

      Intent intent = new Intent("com.bun.msa.action.start.service");
      intent.setClassName("com.mdid.msa", "com.mdid.msa.service.MsaKlService");
      intent.putExtra("com.bun.msa.param.pkgname", pkgName);
      intent.putExtra("com.bun.msa.param.runinset", true);
      if (mContext.startService(intent) != null) {
        Log.d("oaid", "oaid startMsaklServer()  success called with: pkgName = [" + pkgName + "]");
      }
    } catch (Exception e) {
       SigmobLog.e(e.getMessage());
    }
  }

  public void getID(DevicesIDsHelper.AppIdsUpdater _listener) {

    if (!checkService()) {   // 这里等于虚设
      Log.d("oaid", "oaid  checkService false ");
      return;
    }
    String pkgName = mContext.getPackageName();

    startMsaklServer(pkgName);

    Intent v0 = new Intent();
    v0.setClassName("com.mdid.msa", "com.mdid.msa.service.MsaIdService");
    v0.setAction("com.bun.msa.action.bindto.service");
    v0.putExtra("com.bun.msa.param.pkgname", pkgName);
    boolean isBin = mContext.bindService(v0, serviceConnection, Context.BIND_AUTO_CREATE);
    if (isBin) {
      try {
        Log.d("oaid", "oaid getID()  bindService success called with: pkgName = [" + pkgName + "]");

        IBinder iBinder = linkedBlockingQueue.take();
        ZTEIDInterface zteidInterface = new ZTEIDInterface.up.down(iBinder);
        String oaid = zteidInterface.getOAID();
        if (_listener != null) {
          _listener.OnIdsAvalid(oaid);
        }

      } catch (Exception e) {
         SigmobLog.e(e.getMessage());
      }finally {
        mContext.unbindService(serviceConnection);
      }
    }
  }

  public final LinkedBlockingQueue<IBinder> linkedBlockingQueue = new LinkedBlockingQueue(1);
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
