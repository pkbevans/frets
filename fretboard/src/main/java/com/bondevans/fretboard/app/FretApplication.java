package com.bondevans.fretboard.app;

import com.firebase.client.Firebase;

/**
 * @author Paul Evans
 * @since 16/11/15
 *
 * Initialize Firebase with the application context. This must happen before the client is used.
 */
public class FretApplication extends android.app.Application {
    private static final String TAG = FretApplication.class.getSimpleName();
    private String mUID = ""; // Unique User ID

    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }

    public String getUID() {
        return mUID;
    }

    public void setUID(String uID) {
        this.mUID = uID;
    }
}
