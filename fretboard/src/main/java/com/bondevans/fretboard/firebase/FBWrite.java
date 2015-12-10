package com.bondevans.fretboard.firebase;

import android.content.Context;
import android.util.Log;

import com.bondevans.fretboard.firebase.dao.DeviceDetails;
import com.bondevans.fretboard.firebase.dao.Usage;
import com.bondevans.fretboard.fretview.FretSong;
import com.firebase.client.Firebase;

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

    public static void newUser(Context context, Firebase firebaseRef, String uid, String email) {
        firebaseRef.child("users").child(uid).child("email").setValue(email);
        // Also add some device details
        DeviceDetails device = new DeviceDetails(context);
        firebaseRef.child("users").child(uid).push().setValue(device);
    }

    public static void usage(Firebase firebaseRef, String uid, String feature) {
        Usage usage = new Usage(feature);
        firebaseRef.child("usage").child(uid).push().setValue(usage);
    }

    /**
     * Add a new Song to the firebase db
     * @param firebaseRef firebase reference
     * @param fretSong FretSoong to add to db
     * @param description
     */
    public static void addSong(Firebase firebaseRef, FretSong fretSong, String description) {
        // First add the song contents - create a new record
        Firebase contentsRef = firebaseRef.child("songcontents").push();
        // Get the unique ID generated by push()
        String songId = contentsRef.getKey();
        Log.d(TAG, "SongRef=[" + songId + "]");
        Map<String, String> post1 = new HashMap<>();
        post1.put("contents", fretSong.toString());
        contentsRef.setValue(post1);
        // Now store the Song Details entry using id as link to contents
        Firebase detailsRef = firebaseRef.child("songs");
        post1.clear();
        post1.put("id", songId);
        post1.put("name", fretSong.getName());
        post1.put("description", description);
        detailsRef.push().setValue(post1);
    }
}