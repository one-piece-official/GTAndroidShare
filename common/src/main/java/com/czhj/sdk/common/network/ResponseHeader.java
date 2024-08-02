package com.czhj.sdk.common.network;

public enum ResponseHeader {

    LOCATION("Location"),
    USER_AGENT("User-Agent"),
    ACCEPT_LANGUAGE("Accept-Language");

    private final String key;
    ResponseHeader(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

}
