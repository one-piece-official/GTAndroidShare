package com.czhj.sdk.common.Database;

import java.util.List;

public interface SQLiteListener {

    void onSuccess(List<?> list);

    void onFailed(Error error);
}
