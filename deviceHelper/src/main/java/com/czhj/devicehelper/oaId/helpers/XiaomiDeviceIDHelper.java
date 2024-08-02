package com.czhj.devicehelper.oaId.helpers;

import android.content.Context;

import com.czhj.sdk.logger.SigmobLog;

import java.lang.reflect.Method;

/****************************
 * on 2019/10/29
 ****************************
 */
public class XiaomiDeviceIDHelper {
    private Context mContext;

    private Class idProvider;
    private Object idImpl;
    private Method udid;
    private Method oaid;
    private Method vaid;
    private Method aaid;

    public XiaomiDeviceIDHelper(Context ctx) {

        try {
            idProvider = Class.forName("com.android.id.impl.IdProviderImpl");
            idImpl = idProvider.newInstance();
        } catch (Exception e) {
            SigmobLog.e(e.getMessage());
            return;
        }

        mContext = ctx;

        try {
            oaid = idProvider.getMethod("getOAID", new Class[]{Context.class});
        } catch (Exception e) {
            SigmobLog.e(e.getMessage());
            return;

        }
    }

    private String invokeMethod(Context ctx, Method method) {
        String result = null;
        if (idImpl != null && method != null) {
            try {
                result = (String) method.invoke(idImpl, ctx);
            } catch (Exception e) {
                SigmobLog.e(e.getMessage());
            }
        }
        return result;
    }

    public String getOAID() {
        return invokeMethod(mContext, oaid);
    }

}
