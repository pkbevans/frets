package com.bondevans.fretboard.fretboardplayer;

import android.content.Context;

import com.firebase.client.Firebase;

/**
 * @author Paul Evans
 * @since 16/11/15
 *
 * Convenience class for writing stuff to the Firebase NOSQL Database
 */
public class FBWrite {
    public static void newUser(Context context, Firebase firebaseRef, String uid, String email) {
        firebaseRef.child("users").child(uid).child("email").setValue(email);
        // Also add some device details
        DeviceDetails device = new DeviceDetails(context);
        firebaseRef.child("users").child(uid).push().setValue(device);
    }

    public static void usage(Firebase firebaseRef, String uid, String feature) {
        Usage usage = new Usage(Usage.FEATURE_BROWSETO_FILE);
        firebaseRef.child("usage").child(uid).push().setValue(usage);
    }
}
