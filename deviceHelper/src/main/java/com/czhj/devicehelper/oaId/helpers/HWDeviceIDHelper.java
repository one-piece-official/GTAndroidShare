package com.czhj.devicehelper.oaId.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import com.czhj.devicehelper.oaId.interfaces.HWIDInterface;
import com.czhj.sdk.logger.SigmobLog;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;

/****************************
 * *  * on 2019/10/29
 * 获取华为 OAID
 ****************************
 */
public class HWDeviceIDHelper {

    private Context mContext;
    public final LinkedBlockingQueue<IBinder> linkedBlockingQueue = new LinkedBlockingQueue(1);
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public HWDeviceIDHelper(Context ctx) {
        mContext = ctx;
    }

    public void getID(final DevicesIDsHelper.AppIdsUpdater _listener) {

        if (!isSupport()) {
            SigmobLog.e("HWDeviceIDHelper not Support");
            return;
        }

        try {
            // 获取OAID信息（SDK方式）
            // 参阅 https://developer.huawei.com/consumer/cn/doc/HMSCore-Guides/identifier-service-obtaining-oaid-sdk-0000001050064988
            // 华为官方开发者文档提到“调用getAdvertisingIdInfo接口，获取OAID信息，不要在主线程中调用该方法。”
            try {
                Class cls = Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient");
                Method isAdvertisingIdAvailable = cls.getMethod("getAdvertisingIdInfo",Context.class);
                final Object info = isAdvertisingIdAvailable.invoke(null, mContext);
                if (info == null) {
                    SigmobLog.e("HWDeviceIDHelper info is null");

                    String oaid = null;
                    String[] packages = new String[]{"com.huawei.hwid", "com.huawei.hwid.tv", "com.huawei.hms"};
                    for (String pg : packages) {
                        if (TextUtils.isEmpty(oaid)) {
                            oaid = realLoadOaid(pg);
                        }else{
                            if (_listener != null) {
                                _listener.OnIdsAvalid(oaid);
                            }
                            break;
                        }
                    }
                    return;
                }


                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (_listener != null) {
                            try {
                                Method getIdMethod = info.getClass().getDeclaredMethod("getId");
                                Object idObject = getIdMethod.invoke(info);
                                if (idObject instanceof String){
                                    String id = (String) idObject;
                                    _listener.OnIdsAvalid(id);
                                }
                            }catch (Throwable t){

                            }

                        }
                    }
                });
            }catch (Throwable th){
                SigmobLog.e("HWDeviceIDHelper error, will retry");

                String oaid = null;
                String[] packages = new String[]{"com.huawei.hwid", "com.huawei.hwid.tv", "com.huawei.hms"};
                for (String pg : packages) {
                    if (TextUtils.isEmpty(oaid)) {
                        oaid = realLoadOaid(pg);
                    }else{
                        if (_listener != null) {
                            _listener.OnIdsAvalid(oaid);
                        }
                        break;
                    }
                }
            }

        } catch (Throwable th) {
            SigmobLog.e("HWDeviceIDHelper error ",th);

        }
    }

    private String realLoadOaid(String packageName){
        Intent bindIntent = new Intent("com.uodis.opendevice.OPENIDS_SERVICE");
        bindIntent.setPackage(packageName);
        boolean isBin = mContext.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        if (isBin) {
            try {
                IBinder iBinder = linkedBlockingQueue.take();
                HWIDInterface.HWID hwID = new HWIDInterface.HWID(iBinder, mContext);
                String ids = hwID.getIDs();
                return ids;
            } catch (Exception e) {
                SigmobLog.e(e.getMessage());
            } finally {
                mContext.unbindService(serviceConnection);
            }
        }
        return  null;
    }
    public boolean isSupport() {
        try {
            Class cls = Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient");
            Method isAdvertisingIdAvailable = cls.getMethod("isAdvertisingIdAvailable",Context.class);
            Object result = isAdvertisingIdAvailable.invoke(null, mContext);
            if (result instanceof Boolean){
                return (Boolean) result;
            }

        } catch (Throwable e) {

            SigmobLog.e("hw oaid support",e);
        }

        try {
            PackageManager pm = mContext.getPackageManager();

            if (pm.getPackageInfo("com.huawei.hwid", 0) != null) {
                return true;
            }
            if (pm.getPackageInfo("com.huawei.hwid.tv", 0) != null) {
                return true;
            }
            if (pm.getPackageInfo("com.huawei.hms", 0) != null) {
                return true;
            }

        } catch (Throwable e) {

        }

        return false;

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
