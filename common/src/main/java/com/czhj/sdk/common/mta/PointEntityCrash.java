package com.czhj.sdk.common.mta;

public abstract class PointEntityCrash extends PointEntityDevice {

    public String getCrashMessage() {
        return crashMessage;
    }

    public void setCrashMessage(String crashMessage) {
        this.crashMessage = crashMessage;
    }

    private String crashMessage;

    private long crashTime = 0;

    public long getCrashTime() {
        return crashTime;
    }

    public void setCrashTime(long crashTime) {
        this.crashTime = crashTime;
    }
}
