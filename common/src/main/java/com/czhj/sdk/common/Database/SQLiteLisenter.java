package com.czhj.sdk.common.Database;

import java.util.List;

public interface SQLiteLisenter {

    void onSuccess(List<?> list);

    void onFailed(Error error);
}
