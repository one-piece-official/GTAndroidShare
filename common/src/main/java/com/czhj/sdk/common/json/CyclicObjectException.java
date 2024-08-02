package com.czhj.sdk.common.json;

public class CyclicObjectException extends RuntimeException {

    public CyclicObjectException( String str ) {
        super(str);
    }
}
