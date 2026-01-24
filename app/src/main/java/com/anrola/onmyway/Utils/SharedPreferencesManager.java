package com.anrola.onmyway.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.anrola.onmyway.R;

public class SharedPreferencesManager {
    private static volatile SharedPreferencesManager INSTANCE;
    private String DEFAULT_SP_NAME;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private SharedPreferencesManager(Context context) {
        Context appContext = context.getApplicationContext();
        DEFAULT_SP_NAME = appContext.getString(R.string.shared_preferences_token_key);
        sharedPreferences = appContext.getSharedPreferences(DEFAULT_SP_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static SharedPreferencesManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SharedPreferencesManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SharedPreferencesManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public void save(String key,String data) {
        editor.putString(key, data);
        editor.apply();
    }

    public void save(String key,int data) {
        editor.putInt(key, data);
        editor.apply();
    }

    public void save(String key,boolean data) {
        editor.putBoolean(key, data);
        editor.apply();
    }

    public String get(String key) {
        return sharedPreferences.getString(key, null);
    }

    public int getInt(String key) {
        return sharedPreferences.getInt(key, 0);
    }

    public boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public boolean have(String key) {
        return sharedPreferences.contains(key);
    }

    public void clear() {
        editor.clear();
        editor.apply();
    }
}
