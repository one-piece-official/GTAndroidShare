package com.czhj.sdk.common.utils;


import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


public class ViewUtil {

    public static void removeFromParent(View view) {
        if (view == null || view.getParent() == null) {
            return;
        }

        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    public static boolean isViewVisible(View view) {
        return view.getParent() != null && view.getVisibility() == View.VISIBLE && view.isShown();
    }

    public static <T extends View> T findViewByClass(ViewGroup root, Class<T> viewType) {
        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = root.getChildAt(i);
            if (viewType.isInstance(child)) {
                return viewType.cast(child);
            } else if (child instanceof ViewGroup) {
                T result = findViewByClass((ViewGroup) child, viewType);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Finds the topmost view in the current Activity or current view hierarchy.
     *
     * @param context If an Activity Context, used to obtain the Activity's DecorView. This is
     *                ignored if it is a non-Activity Context.
     * @param view    A View in the currently displayed view hierarchy. If a null or non-Activity
     *                Context is provided, this View's topmost parent is used to determine the
     *                rootView.
     * @return The topmost View in the currency Activity or current view hierarchy. Null if no
     * applicable View can be found.
     */

    public static View getTopmostView(final Context context, final View view) {
        final View rootViewFromActivity = getRootViewFromActivity(context);
        final View rootViewFromView = getRootViewFromView(view);

        // Prefer to use the rootView derived from the Activity's DecorView since it provides a
        // consistent value when the View is not attached to the Window. Fall back to the passed-in
        // View's hierarchy if necessary.
        return rootViewFromActivity != null ? rootViewFromActivity : rootViewFromView;
    }

    public static Activity getActivityFromViewTop(View view) {

        View rootView = getRootViewFromView(view);
        if (rootView != null) {
            return getActivityFromView(rootView);
        }

        return null;
    }

    private static View getRootViewFromActivity(final Context context) {
        if (!(context instanceof Activity)) {
            return null;
        }

        return ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
    }

    public static Activity getActivityFromView(View view) {
        if (view != null) {
            Context context = view.getContext();
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) {
                    return (Activity) context;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }

        }
        return null;
    }

    public static View getRootViewFromView(final View view) {
        if (view == null) {
            return null;
        }

        final View rootView = view.getRootView();

        if (rootView == null) {
            return null;
        }

        final View rootContentView = rootView.findViewById(android.R.id.content);
        return rootContentView != null ? rootContentView : rootView;
    }

    //判断点击是否还在该view中，是返回true ,不是返回false
    public static boolean isPointInView(View view, MotionEvent ev) {
        if (view == null) {
            return false;
        }
        //控件自身左上角的X坐标
        int x = (int) ev.getRawX();
        //控件自身左上角的Y坐标
        int y = (int) ev.getRawY();
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        if (y >= top && y <= bottom && x >= left && x <= right) {
            return true;
        }
        return false;
    }


//    public TouchLocation getTouchX(View adContainer, MotionEvent event) {
//        int[] location=new int[2];
//        adContainer.getLocationInWindow(location);
//        int x = location[0];
//        int y = location[1];
//
//    }
}
