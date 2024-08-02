package com.czhj.devicehelper.oaId.helpers;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.czhj.sdk.logger.SigmobLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;

/****************************
 * on 2019/10/28
 ****************************
 */
public class DevicesIDsHelper {

    private static AppIdsUpdater _listener;
    private static String mOaidCertPem;
    private AppIdsUpdater _oaidlistener;

    private static int MSASupportResult = -1;
    private static int oaidRetryCount = 0;

    private static String mOAID;

    private static String getBrand() {
        return Build.BRAND.toUpperCase();
    }

    private static String getManufacturer() {
        return Build.MANUFACTURER.toUpperCase();
    }

    public void getOAIDSrc(Context mcontext, AppIdsUpdater callback) {

        String oaid = null;
        boolean isSupport = false;

        _oaidlistener = callback;
        SigmobLog.e("getManufacturer ===> " + getManufacturer());

        String manufacturer = getManufacturer().toUpperCase();

        if ("ASUS".equals(manufacturer)) {

            getIDFromNewThead(mcontext, manufacturer);
        } else if ("HUAWEI".equals(manufacturer)
                || "HONOR".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("HUAWEI")
                || Build.BRAND.equalsIgnoreCase("HONOR")
                || isEmuiOs()) {

            getIDFromNewThead(mcontext, "HUAWEI");
        } else if ("LENOVO".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("ZUK")) {
            getIDFromNewThead(mcontext, "LENOVO");
        } else if ("MOTOLORA".equals(manufacturer)) {
            getIDFromNewThead(mcontext, manufacturer);

        } else if ("MEIZU".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("MEIZU")
                || Build.DISPLAY.toUpperCase().contains("FLYME")) {

            getIDFromNewThead(mcontext, "MEIZU");

        } else if ("NUBIA".equals(manufacturer)) {
            oaid = new NubiaDeviceIDHelper(mcontext).getNubiaID();

        } else if ("OPPO".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("OPPO")
                || Build.BRAND.equalsIgnoreCase("REALME")) {
            getIDFromNewThead(mcontext, "OPPO");

        } else if ("SAMSUNG".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("SAMSUNG")) {

            getIDFromNewThead(mcontext, "SAMSUNG");

        } else if ("VIVO".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("VIVO")
                || !TextUtils.isEmpty(getProperty("ro.vivo.os.version"))) {
            oaid = new VivoDeviceIDHelper(mcontext).getOaid();

        } else if ("XIAOMI".equals(manufacturer)
                || "BLACKSHARK".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("XIAOMI")
                || Build.BRAND.equalsIgnoreCase("REDMI")
                || isMiuiOs()) {

            oaid = new XiaomiDeviceIDHelper(mcontext).getOAID();

        } else if ("ONEPLUS".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("ONEPLUS")) {
            getIDFromNewThead(mcontext, "ONEPLUS");
        } else if ("ZTE".equals(manufacturer)
                || Build.BRAND.equalsIgnoreCase("ZTE")) {
            getIDFromNewThead(mcontext, "ZTE");

        } else if ("FERRMEOS".equals(manufacturer) || isFreeMeOS()) {
            getIDFromNewThead(mcontext, "FERRMEOS");

        } else if ("SSUI".equals(manufacturer) || isSSUIOS()) {
            getIDFromNewThead(mcontext, "SSUI");

        } else {
            getIDFromNewThead(mcontext, manufacturer);
        }

        if (oaid != null) {
            isSupport = true;
        }

        if (_oaidlistener != null) {
            _oaidlistener.OnIdsAvalid(oaid);
        }
    }


    private static String getProperty(String property) {
        String res = null;
        if (property == null) {
            return null;
        }
        try {
            Class clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", new Class[]{String.class, String.class});
            res = (String) method.invoke(clazz, new Object[]{property, ""});
        } catch (Exception e) {
            // ignore
        }

        return res;
    }


    public static boolean isEmuiOs(){
        String pro = getProperty("ro.build.version.emui");      // "ro.miui.ui.version.name"
        if ((!TextUtils.isEmpty(pro))) {
            return true;
        }
        return false;
    }

    public static boolean isMiuiOs(){
        String pro = getProperty("ro.miui.ui.version.name");      // "ro.miui.ui.version.name"
        if ((!TextUtils.isEmpty(pro))) {
            return true;
        }
        return false;
    }
    public static boolean isFreeMeOS() {
        String pro = getProperty("ro.build.freeme.label");      // "ro.build.freeme.label"
        if ((!TextUtils.isEmpty(pro)) && pro.equalsIgnoreCase("FREEMEOS")) {      // "FreemeOS"  FREEMEOS
            return true;
        }
        return false;
    }

    public static boolean isSSUIOS() {
        String pro = getProperty("ro.ssui.product");    // "ro.ssui.product"
        if ((!TextUtils.isEmpty(pro)) && (!pro.equalsIgnoreCase("unknown"))) {
            return true;
        }
        return false;
    }


    /**
     * 启动子线程获取
     *
     * @param context
     */
    private void getIDFromNewThead(final Context context, final String manufacturerm) {

        Log.d("", "Thread create ,current thread num :" + Thread.activeCount());

        new Thread(new Runnable() {

            @Override
            public void run() {
                if ("ASUS".equals(manufacturerm)) {
                    new ASUSDeviceIDHelper(context).getID(_oaidlistener);

                } else if ("HUAWEI".equals(manufacturerm) || "HONOR".equalsIgnoreCase(manufacturerm)) {

                    HIDeviceIDHelper hiDeviceIDHelper = new HIDeviceIDHelper(context);
                    if (hiDeviceIDHelper.isSupport()){
                        hiDeviceIDHelper.getID(_oaidlistener);
                        return;
                    }

                    new HWDeviceIDHelper(context).getID(_oaidlistener);

                } else if ("OPPO".equals(manufacturerm)) {
                    new OppoDeviceIDHelper(context).getID(_oaidlistener);

                } else if ("LENOVO".equals(manufacturerm)) {
                    new LenovoDeviceIDHelper(context).getIdRun(_oaidlistener);

                } else if ("MOTOLORA".equals(manufacturerm)) {
                    new LenovoDeviceIDHelper(context).getIdRun(_oaidlistener);

                } else if ("MEIZU".equals(manufacturerm)) {
                    new MeizuDeviceIDHelper(context).getMeizuID(_oaidlistener);

                } else if ("SAMSUNG".equals(manufacturerm)) {
                    new SamsungDeviceIDHelper(context).getSumsungID(_oaidlistener);

                } else if ("ONEPLUS".equals(manufacturerm)) {
                    new OnePlusDeviceIDHelper(context).getID(_oaidlistener);

                } else if ("ZTE".equals(manufacturerm)) {
                    new ZTEDeviceIDHelper(context).getID(_oaidlistener);

                } else if ("FERRMEOS".equals(manufacturerm) || isFreeMeOS()) {
                    new ZTEDeviceIDHelper(context).getID(_oaidlistener);

                } else if ("SSUI".equals(manufacturerm) || isSSUIOS()) {
                    new ZTEDeviceIDHelper(context).getID(_oaidlistener);
                } else {
                    new ZTEDeviceIDHelper(context).getID(_oaidlistener);
                }
            }
        }).start();
    }


    private static Class<?> mIdentifyListener;
    private static Class<?> mIdSupplier;
    private static Class<?> jLibrary;
    private static Class<?> mMidSDKHelper;
    /**
     * 自定义 oaid 证书文件在 assets 中的路径
     */
    private static String mOidCertFilePath;
    private static final List<String> mBlackOAIDs = new LinkedList<String>() {
        {
            add("00000000-0000-0000-0000-000000000000");
            add("00000000000000000000000000000000");
        }
    };
    private static final List<String> mLoadLibrary = new LinkedList<String>() {
        {
            add("msaoaidsec");                  // v1.0.30
            add("nllvm1632808251147706677");    // v1.0.29
            add("nllvm1630571663641560568");    // v1.0.27
            add("nllvm1623827671");             // v1.0.26
        }
    };

    static {
        initSDKLibrary();
    }

    /**
     * 获取 OAID 接口，注意该接口是同步接口，可能会导致线程阻塞，建议在子线程中使用
     *
     * @param context Context
     * @return OAID
     */
    public static void getOAID(final Context context, AppIdsUpdater appIdsUpdater) {
        _listener = appIdsUpdater;
        getRomOAID(context);


    }

    private static void getRomOAID(final Context context) {
        try {
            initInvokeListener();
            if (context == null || mMidSDKHelper == null || mIdentifyListener == null || mIdSupplier == null) {
                SigmobLog.e("OAID 读取类创建失败");
                if (_listener != null) {
                    _listener.OnIdsAvalid(mOAID);
                }
                return;
            }
            if (TextUtils.isEmpty(mOAID)) {
                getOAIDReflect(context);
            } else {
                if (_listener != null) {
                    _listener.OnIdsAvalid(mOAID);
                }
            }
        } catch (Throwable ex) {
            SigmobLog.e(ex.getMessage());
            if (_listener != null) {
                _listener.OnIdsAvalid(mOAID);
            }
        }
    }

    /**
     * 通过反射获取 OAID，结果返回的 ErrorCode 如下：
     * 1008611：不支持的设备厂商
     * 1008612：不支持的设备
     * 1008613：加载配置文件出错
     * 1008614：获取接口是异步的，结果会在回调中返回，回调执行的回调可能在工作线程
     * 1008615：反射调用出错
     *
     * @param context Context
     */
    private static void getOAIDReflect(Context context) {
        try {

            final int INIT_ERROR_RESULT_DELAY = 1008614;            //获取接口是异步的，结果会在回调中返回，回调执行的回调可能在工作线程
            final int INIT_ERROR_RESULT_OK = 1008610;            //获取成功
            // 初始化证书
            initPemCert(context);

            // 初始化 Library
            try {
                if (jLibrary != null) {
                    Field field = jLibrary.getField("classLoader");
                    Object classLoader = field.get(jLibrary);
                    if (classLoader == null) {
                        Method initEntry = jLibrary.getDeclaredMethod("InitEntry", Context.class);
                        initEntry.invoke(jLibrary, context);
                    }

                }
            } catch (Exception e) {
//            e.printStackTrace();
            }

            IdentifyListenerHandler handler = new IdentifyListenerHandler();
            Method initSDK = mMidSDKHelper.getDeclaredMethod("InitSdk", Context.class, boolean.class, mIdentifyListener);
            int errCode = (int) initSDK.invoke(null, context, true, Proxy.newProxyInstance(context.getClassLoader(), new Class[]{mIdentifyListener}, handler));
            SigmobLog.e("MdidSdkHelper ErrorCode : " + errCode);
            if (errCode != INIT_ERROR_RESULT_DELAY && errCode != INIT_ERROR_RESULT_OK) {
                if (_listener != null) {
                    _listener.OnIdsAvalid(mOAID);
                }
            }

        } catch (Throwable ex) {
            SigmobLog.e(ex.getMessage());
            if (_listener != null) {
                _listener.OnIdsAvalid(mOAID);
            }
        }
    }

    static class IdentifyListenerHandler implements InvocationHandler {


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                if ("OnSupport".equalsIgnoreCase(method.getName())) {
                    Method getOAID = mIdSupplier.getDeclaredMethod("getOAID");

                    if (args.length == 1) {// v1.0.26 版本只有 1 个参数
                        mOAID = (String) getOAID.invoke(args[0]);
                    } else {
                        mOAID = (String) getOAID.invoke(args[1]);
                    }
                    if (_listener != null) {
                        _listener.OnIdsAvalid(mOAID);
                    }
                    SigmobLog.e("MdidSdkHelper oaid:" + mOAID);
                }
            } catch (Throwable ex) {
                if (_listener != null) {
                    _listener.OnIdsAvalid(mOAID);
                }
            }
            return null;
        }
    }

    private static void initInvokeListener() {


        try {
            mMidSDKHelper = Class.forName("com.bun.miitmdid.core.MdidSdkHelper");
        } catch (ClassNotFoundException e) {
            SigmobLog.e(e.getMessage());
            return;
        }
        // 尝试 1.0.22 版本
        try {
            mIdentifyListener = Class.forName("com.bun.miitmdid.interfaces.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.miitmdid.interfaces.IdSupplier");
            return;
        } catch (Exception ex) {
            // ignore
        }

        // 尝试 1.0.13 - 1.0.21 版本
        try {
            mIdentifyListener = Class.forName("com.bun.supplier.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.supplier.IdSupplier");
            jLibrary = Class.forName("com.bun.miitmdid.core.JLibrary");
            return;
        } catch (Exception ex) {
            // ignore
        }

        // 尝试 1.0.5 - 1.0.13 版本
        try {
            mIdentifyListener = Class.forName("com.bun.miitmdid.core.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.miitmdid.supplier.IdSupplier");
            jLibrary = Class.forName("com.bun.miitmdid.core.JLibrary");
        } catch (Exception ex) {
            // ignore
        }
    }

    private static void initSDKLibrary() {
        for (String library : mLoadLibrary) {
            try {
                System.loadLibrary(library);  // 加载 SDK 安全库
                break;
            } catch (Throwable throwable) {
                // ignore
            }
        }
    }

    /**
     * 初始化证书
     *
     * @param context Context
     */
    private static void initPemCert(Context context) {
        try {
            String oaidCert = !TextUtils.isEmpty(mOaidCertPem) ? mOaidCertPem : loadPemFromAssetFile(context);
            if (!TextUtils.isEmpty(oaidCert)) {
                Method initCert = mMidSDKHelper.getDeclaredMethod("InitCert", Context.class, String.class);
                initCert.invoke(null, context, oaidCert);
            }
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
    }

    /**
     * 从asset文件读取证书内容
     *
     * @param context Context
     * @return 证书字符串
     */
    private static String loadPemFromAssetFile(Context context) {
        try {

            InputStream is;
            String defaultPemCert = context.getPackageName() + ".cert.pem";
            AssetManager assetManager = context.getAssets();

            if (!TextUtils.isEmpty(mOidCertFilePath)) {
                try {
                    is = assetManager.open(mOidCertFilePath);
                } catch (IOException e) {
                    is = assetManager.open(defaultPemCert);
                }
            } else {
                is = assetManager.open(defaultPemCert);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            SigmobLog.d("loadPemFromAssetFile failed");
            return "";
        }
    }

    public static void setOAIDCertPem(String certPem) {
        mOaidCertPem = certPem;
    }

    public static void setPemCustomFileName(String fileName) {
        mOidCertFilePath = fileName;
    }

    public interface AppIdsUpdater {
        void OnIdsAvalid(String oaid);
    }

}
