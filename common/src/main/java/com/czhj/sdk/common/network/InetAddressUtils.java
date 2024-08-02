package com.czhj.sdk.common.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

class InetAddressUtils {

    public static InetAddress getInetAddressByName( final String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }

    private InetAddressUtils() {
    }
}
