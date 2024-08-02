package com.czhj.devicehelper.oaId.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;

import com.czhj.devicehelper.oaId.interfaces.LenovoIDInterface;
import com.czhj.sdk.logger.SigmobLog;

import java.util.concurrent.LinkedBlockingQueue;

/****************************
 * on 2019/10/29
 * 获取联想 OAID
 ****************************
 */
public class LenovoDeviceIDHelper {

  private Context mContext;
  LenovoIDInterface lenovoIDInterface;
  public final LinkedBlockingQueue<IBinder> linkedBlockingQueue = new LinkedBlockingQueue(1);

  public LenovoDeviceIDHelper(Context ctx) {
    mContext = ctx;
  }

  public void getIdRun(DevicesIDsHelper.AppIdsUpdater _listener) {

    if(!isSupport()){
      return;
    }
    Intent intent = new Intent();
    intent.setClassName("com.zui.deviceidservice", "com.zui.deviceidservice.DeviceidService");
    boolean seu = mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    if (seu) {

      try {
        IBinder  service = linkedBlockingQueue.take();
        lenovoIDInterface = new LenovoIDInterface.len_up.len_down(service);

        if (lenovoIDInterface != null) {
          String oaid = lenovoIDInterface.a();
          if (_listener != null) {
            _listener.OnIdsAvalid(oaid);
          }
        }
      } catch (Throwable e) {
        SigmobLog.e(e.getMessage());
      }finally{
        mContext.unbindService(serviceConnection);
      }

    }
  }

  ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      try {

        linkedBlockingQueue.put(service);
      }catch (Throwable th){

      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
  };

  private boolean isSupport(){
    try {
      PackageInfo pi = mContext.getPackageManager().getPackageInfo("com.zui.deviceidservice", 0);
      return pi != null;
    } catch (Throwable e) {
      return false;
    }
  }
}
