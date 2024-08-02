package com.czhj.sdk.common.network;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;

import com.czhj.sdk.common.utils.DeviceUtils;
import com.czhj.volley.RequestQueue;
import com.czhj.volley.toolbox.ImageLoader;

class MaxWidthImageLoader extends ImageLoader {
    private final int mMaxImageWidth;


    MaxWidthImageLoader(final RequestQueue queue, final Context context, final ImageCache imageCache) {
        super(queue, imageCache);

        // Get Display Options
        Display display = DeviceUtils.getDisplay(context);

        if (display == null) {
            mMaxImageWidth = 320;
            return;
        }

        Point size = new Point();
        display.getSize(size);

        // Make our images no wider than the skinny side of the display.
        mMaxImageWidth = Math.min(size.x, size.y);
    }

    @Override
    public ImageContainer get(final String requestUrl, final ImageListener listener) {
        return super.get(requestUrl, listener, mMaxImageWidth, 0 /* no height limit */);
    }
}

