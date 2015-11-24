package com.bondevans.fretboard.app;

import com.firebase.client.Firebase;

/**
 * @author Paul Evans
 * @since 16/11/15
 *
 * Initialize Firebase with the application context. This must happen before the client is used.
 */
public class FretboardApplication extends android.app.Application {
    private static final String TAG = "FretboardApplication";
    private String mAuthID; // Unique User ID

    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }

    public String getAuthID() {
        return mAuthID;
    }

    public void setAuthID(String authID) {
        this.mAuthID = authID;
    }
}
