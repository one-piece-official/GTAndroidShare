package com.czhj.devicehelper.oaId.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.czhj.sdk.logger.SigmobLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;

/****************************
 * *  * on 2019/10/29
 * 获取华为 OAID
 ****************************
 */
public class HIDeviceIDHelper {

    private Context mContext;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public final LinkedBlockingQueue<IBinder> linkedBlockingQueue = new LinkedBlockingQueue(1);

    public HIDeviceIDHelper(Context ctx) {
        mContext = ctx;
    }

    public void getID(final DevicesIDsHelper.AppIdsUpdater _listener) {

        if (!isSupport()) {
            SigmobLog.e("HIDeviceIDHelper not Support");

            return;
        }

        try {
            // 获取OAID信息（SDK方式）
            // 参阅 https://developer.huawei.com/consumer/cn/doc/HMSCore-Guides/identifier-service-obtaining-oaid-sdk-0000001050064988
            // 华为官方开发者文档提到“调用getAdvertisingIdInfo接口，获取OAID信息，不要在主线程中调用该方法。”

            Class cls = Class.forName("com.hihonor.ads.identifier.AdvertisingIdClient");
            Method getAdvertisingIdInfoMethod = cls.getMethod("getAdvertisingIdInfo",Context.class);
            final Object info = getAdvertisingIdInfoMethod.invoke(null, mContext);

            if (info == null) {
                SigmobLog.e("HIDeviceIDHelper info is null");
                return;
            }

            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (_listener != null) {
                        try {
                            Field id = info.getClass().getField("id");

                            Object o = id.get(info);
                            if (o instanceof String){
                                _listener.OnIdsAvalid((String) o);
                            }
                        } catch (Throwable e) {

                        }


                    }
                }
            });
        } catch (Throwable th) {
            SigmobLog.e("HIDeviceIDHelper error ",th);

        }
    }


    public boolean isSupport() {
        try {
            try {
                Class cls = Class.forName("com.hihonor.ads.identifier.AdvertisingIdClient");
                Method isAdvertisingIdAvailable = cls.getMethod("isAdvertisingIdAvailable",Context.class);
                Object result = isAdvertisingIdAvailable.invoke(null, mContext);
                if (result instanceof Boolean){
                    return (Boolean) result;
                }

            } catch (Throwable e) {
                SigmobLog.e("HIDeviceIDHelper isAdvertisingIdAvailable error ", e);

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
