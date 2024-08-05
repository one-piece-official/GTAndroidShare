package com.czhj.sdk.common.utils;

import static android.content.Context.MODE_MULTI_PROCESS;
import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.czhj.sdk.common.Constants;

public final class SharedPreferencesUtil {

    private static String DEFAULT_PREFERENCE_NAME = "com." + Constants.SDK_COMMON_FOLDER + ".setting";

    private SharedPreferencesUtil(String default_preference_name) {

        DEFAULT_PREFERENCE_NAME = default_preference_name;
    }

    public static SharedPreferences getSharedPreferences(final Context context) {
        Preconditions.NoThrow.checkNotNull(context);

        return context.getSharedPreferences(DEFAULT_PREFERENCE_NAME, MODE_PRIVATE | MODE_MULTI_PROCESS);
    }

    public static SharedPreferences getSharedPreferences(final Context context, final String preferenceName) {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(preferenceName);

        return context.getSharedPreferences(preferenceName, MODE_PRIVATE | MODE_MULTI_PROCESS);
    }

}
