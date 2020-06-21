package com.bondevans.frets.app;

import android.content.Context;

import com.bondevans.frets.firebase.dao.UserProfile;

/**
 * @author Paul Evans
 * @since 16/11/15
 *
 */
public class FretApplication extends android.app.Application {
    private static final String TAG = FretApplication.class.getSimpleName();
    private static String mUID = ""; // Unique User ID
    private static Context appContext;
    private static UserProfile userProfile;


    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return appContext;
    }
    public static String getUserName() {
        return userProfile.getUsername();
    }
    public static String getUID() {
        return mUID;
    }
    public void setUser(String uID, UserProfile userProfile) {
        this.mUID = uID;
        this.userProfile = userProfile;
    }
}
