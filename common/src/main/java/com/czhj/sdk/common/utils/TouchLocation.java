package com.czhj.sdk.common.utils;

import android.view.MotionEvent;
import android.view.View;

import java.io.Serializable;

public class TouchLocation implements Serializable {

    private final int x;
    private final int y;

    public TouchLocation(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public static TouchLocation getTouchLocation(View adContainer, MotionEvent event) {

        if (adContainer != null && event != null) {
            //控件自身左上角的X坐标
            int x = (int) event.getRawX();
            //控件自身左上角的Y坐标
            int y = (int) event.getRawY();
            int[] location = new int[2];
            adContainer.getLocationOnScreen(location);
            int left = location[0];
            int top = location[1];

            return new TouchLocation(x - left, y - top);
        }
        return null;


    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
