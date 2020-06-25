package com.bondevans.frets.firebase.dao;

import com.bondevans.frets.utils.Log;
import com.google.firebase.database.Exclude;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")

public class UserProfile {
    private static final String TAG = UserProfile.class.getSimpleName();
    public static String childName = UserProfile.class.getSimpleName().toLowerCase();
    String  username;
    String  email;
    String  bio;
    String  website;
    long    dateJoined;
    private boolean updated;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public UserProfile() {
        this.username = "";
        this.email = "";
        this.bio = "";
        this.website = "";
        this.dateJoined = 0;
        this.updated = false;
    }

    public UserProfile(String username, String email, String  bio, String  website, long dateJoined) {
        this.username = username;
        this.email = email;
        this.bio = bio;
        this.website = website;
        this.dateJoined = dateJoined;
        this.updated = false;
    }

    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public String getBio() {
        return bio;
    }
    public long getDateJoined() {
        return dateJoined;
    }
    public String getWebsite() {
        return website;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.updated = !username.contentEquals(this.username);
        this.username = username;
    }
    public void setBio(String bio) {
        this.updated = !bio.contentEquals(this.bio);
        this.bio = bio;
    }
    public void setWebsite(String website) {
        this.updated = !website.contentEquals(this.website);
        this.website = website;
    }
    public void setDateJoined(long dateJoined) {
        this.dateJoined = dateJoined;
    }
    @Exclude
    public boolean isUpdated(){
        return updated;
    }

    public static String getUsername(String userProfileJson) {
        try {
            JSONObject jsonObject = new JSONObject(userProfileJson);
            return jsonObject.getString("username");
        } catch (JSONException e) {
            Log.e(TAG, "HELLO JSON ERROR" + e.getMessage());
        }
        return "";
    }

    @Override
    public String toString() {
        return "{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", bio='" + bio + '\'' +
                ", website='" + website + '\'' +
                ", dateJoined=" + dateJoined +
                ", updated=" + updated +
                '}';
    }
}