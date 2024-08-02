package com.czhj.sdk.common.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Dips {
    private static float pixelsToFloatDips(final float pixels, final Context context) {
        return pixels / getDensity(context);
    }

    public static int pixelsToIntDips(final float pixels, final Context context) {
        if (pixels == 0 || context == null){
            return 0;
        }
        return (int) (pixelsToFloatDips(pixels, context) + 0.5f);
    }

    private static float dipsToFloatPixels(final float dips, final Context context) {
        return dips * getDensity(context);
    }

    public static int dipsToIntPixels(final float dips, final Context context) {
        if (dips == 0 || context == null){
            return 0;
        }
        return (int) (dipsToFloatPixels(dips, context) + 0.5f);
    }

    public static float getDensity(final Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    private static float asFloatPixels(float dips, Context context) {
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, displayMetrics);
    }
    
    public static int asIntPixels(float dips, Context context) {
        if (dips == 0 || context == null){
            return 0;
        }
        return (int) (asFloatPixels(dips, context) + 0.5f);
    }

    public static int screenWidthAsIntDips(Context context) {
        if (context == null){
            return 0;
        }
        return pixelsToIntDips(context.getResources().getDisplayMetrics().widthPixels, context);
    }

    public static int screenHeightAsIntDips(Context context) {
        if (context == null){
            return 0;
        }
        return pixelsToIntDips(context.getResources().getDisplayMetrics().heightPixels, context);
    }
}
