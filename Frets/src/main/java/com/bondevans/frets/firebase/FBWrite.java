package com.bondevans.frets.firebase;

import android.util.Log;

import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.firebase.dao.Fret;
import com.bondevans.frets.firebase.dao.FretClick;
import com.bondevans.frets.firebase.dao.UserProfile;
import com.bondevans.frets.fretview.FretSong;
import com.google.firebase.database.DatabaseReference;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Paul Evans
 * @since 16/11/15
 *
 * Convenience class for writing stuff to the Firebase NOSQL Database
 */
public class FBWrite {
    private static final String TAG = FBWrite.class.getSimpleName();

    public static void addUserProfile(DatabaseReference firebaseRef, UserProfile userProfile, String uid) {
        firebaseRef.child("users").child(uid).child("userProfile").setValue(userProfile);
    }

    public static void updateUser(DatabaseReference firebaseRef, UserProfile userProfile, String uid) {
        firebaseRef.child("users").child(uid).child("userProfile").setValue(userProfile);
    }
    public static void usage(DatabaseReference firebaseRef, String uId, String fretId) {
        FretClick fretClick = new FretClick(fretId, uId);
        firebaseRef.child(FretClick.childName).child(uId).push().setValue(fretClick);
    }

    /**
     * Add a new Fret to the firebase db
     * @param firebaseRef firebase reference
     * @param fretSong FretSong to add to db
     * @param uid User Id
     */
    public static void addPrivateFret(DatabaseReference firebaseRef, FretSong fretSong, String uid) {
        // First add the fret contents - create a new record
        DatabaseReference contentsRef = firebaseRef.child("fretcontents").push();
        // Get the unique ID generated by push()
        String fretId = contentsRef.getKey();
        Log.d(TAG, "FretRef=[" + fretId + "]");
        Map<String, String> post1 = new HashMap<>();
        post1.put("contents", fretSong.toString());
        contentsRef.setValue(post1);
        // Now store the Song Details entry using id as link to contents
        DatabaseReference detailsRef = firebaseRef.child("users").child(uid).child("frets");
        // Add uploadedBy, dateUploaded, etc details
        // TODO - get instrument from FretSong
        detailsRef.push().setValue(new Fret(fretId, fretSong.getName(), fretSong.getKeywords(),uid, FretApplication.getUserName(),Fret.LEAD_GUITAR, new Date().getTime() ));
    }

    /**
     * Add a new Fret to the firebase db
     * @param firebaseRef firebase reference
     * @param fret Fret to add to db
     * @param uid User Id
     */
    public static void publishPrivateFret(DatabaseReference firebaseRef, Fret fret, String uid, String fretRef) {
        // Create the Fret Details entry in the Public area - using id as link to contents
        DatabaseReference dbRef = firebaseRef.child("frets");
        dbRef.push().setValue(fret);
        // Remove the private version
        dbRef = firebaseRef.child("users").child(uid).child("frets").child(fretRef);
        dbRef.removeValue();
    }

    /**
     * Delete a Fret from the firebase db
     * @param firebaseRef firebase reference
     * @param uid User Id
     * @param fretRef fret reference
     */
    @SuppressWarnings("unused")
    public static void deletePrivateFret(DatabaseReference firebaseRef, String uid, String fretRef) {
        Log.d(TAG, "HELLO deletePrivateFret id=[" + fretRef + "] user Id=["+uid+"]");
        // First remove the fret
        DatabaseReference dbRef = firebaseRef.child("users").child(uid).child("frets").child(fretRef);
        dbRef.removeValue();
        // Now the song contents
        dbRef = firebaseRef.child("fretcontents").child(fretRef);
        dbRef.removeValue();
    }
}
