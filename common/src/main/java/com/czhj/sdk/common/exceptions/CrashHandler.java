package com.czhj.sdk.common.exceptions;

import static android.os.Process.killProcess;

import android.os.Process;
import android.text.TextUtils;

import com.czhj.sdk.common.mta.PointEntityCrash;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Lance at 2020/5/21
 * I must be One Piece !
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH = "crash";
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;

    private static CrashHandler gInstance = null;
    private PointEntityCrash entityCrash = null;

    private Set<CrashHandlerListener> crashHandlerListenerSet = new HashSet();

    private CrashHandler(){
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void add(CrashHandlerListener crashHandlerListener){

        if (crashHandlerListener != null)
            crashHandlerListenerSet.add(crashHandlerListener);

    }

    public static synchronized CrashHandler getInstance() {

        if (gInstance == null){
            synchronized (CrashHandler.class){
                gInstance = new CrashHandler();
            }
        }
         return gInstance;
    }


    /**
     * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用uncaughtException方法
     * thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息
     *
     * @param thread
     * @param ex
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            handleException(ex);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        //如果系统提供默认的异常处理器，则交给系统去结束程序，否则就由自己结束自己
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            //自己处理
            killProcess(Process.myPid());
        }
    }


    private boolean handleException(final Throwable exc) {
        if (exc == null) {
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //处理崩溃
                writeCrash(exc);
            }
        }).start();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }


    /**
     * 采集崩溃原因
     * OutOfMemoryError
     *
     * @param exc 异常
     */
    private void writeCrash(Throwable exc) {
        try {
            Writer writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            exc.printStackTrace(pw);
            Throwable excCause = exc.getCause();
            while (excCause != null) {
                excCause.printStackTrace(pw);
                excCause = excCause.getCause();
            }
            pw.close();
            String crash = writer.toString();
            if (!TextUtils.isEmpty(crash)) {
                for (CrashHandlerListener listener: this.crashHandlerListenerSet) {
                    if (listener != null){
                        listener.reportCrash(crash);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public interface CrashHandlerListener {
        void reportCrash(String crash);
    }









}
