package com.tsclown.permission;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

/**
 * SharedPreferences辅助类
 */

public class PreferencesHelper {
    private static final String SP_NAME = "sp_cxedu_works_permission";

    public static final String SP_KEY_PERMISSION_IS_FIRST_REQUEST = "sp_key_permission_is_first_request";

    private PreferencesHelper() {
    }

    private static SharedPreferences mSharedPreferences = null;

    public static void init(Context appContext) {
        if (mSharedPreferences == null) {
            mSharedPreferences =
                    appContext.getSharedPreferences(SP_NAME,
                            Context.MODE_PRIVATE);
        }
    }

    public static void putStringSet(String key, Set<String> value) {
        mSharedPreferences.edit().putStringSet(key, value).apply();
    }

    public static Set<String> getStringSet(String key, Set<String> defaultValue) {
        return mSharedPreferences.getStringSet(key, defaultValue);
    }

    public static Set<String> getStringSet(String key) {
        return mSharedPreferences.getStringSet(key, null);
    }

    public static void release() {
        mSharedPreferences = null;
    }

}
