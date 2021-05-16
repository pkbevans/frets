package com.bondevans.frets.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Pref {
    public static final String SYNTH = "SYNTH";

    public static void setPreference(Activity activity, String key, String value){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        // Commit the edits!
        editor.commit();
    }
    public static void removePreference(Activity activity, String key ){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);

        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        // Commit the edits!
        editor.commit();
    }
    public static String getPreference(Activity activity, String key){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        return settings.getString(key, "");
    }
}
