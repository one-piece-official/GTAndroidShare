package com.czhj.sdk.common.network;

import com.czhj.volley.toolbox.HurlStack;

class PlayServicesUrlRewriter implements HurlStack.UrlRewriter {

    private static final String UDID_TEMPLATE = "mp_tmpl_advertising_id";
    private static final String DO_NOT_TRACK_TEMPLATE = "mp_tmpl_do_not_track";

    PlayServicesUrlRewriter() {
    }

    @Override
    public String rewriteUrl(final String url) {
        if (!url.contains(UDID_TEMPLATE) && !url.contains(DO_NOT_TRACK_TEMPLATE)) {
            return url;
        }

        return url;
    }

}
