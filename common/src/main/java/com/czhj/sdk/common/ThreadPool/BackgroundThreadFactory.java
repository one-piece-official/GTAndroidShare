package com.czhj.sdk.common.ThreadPool;


import com.czhj.sdk.logger.SigmobLog;

import java.util.concurrent.ThreadFactory;

public class BackgroundThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {

        SigmobLog.d("ThreadFactor create ,current thread num :" + Thread.activeCount());
        return new Thread(r) {
            @Override
            public void run() {
                // 设置线程的优先级
                super.run();
            }
        };
    }

}
