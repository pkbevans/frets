package com.bondevans.frets.firebase.dao;
@SuppressWarnings("unused")

public class UserProfile {
    public static String childName = UserProfile.class.getSimpleName().toLowerCase();
    String  username;
    String  email;
    String  bio;
    String  website;
    long    dateJoined;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public UserProfile() {
    }

    public UserProfile(String username, String email, String  bio, String  website, long dateJoined) {
        this.username = username;
        this.email = email;
        this.bio = bio;
        this.website = website;
        this.dateJoined = dateJoined;
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
    public void setBio(String bio) {
        this.bio = bio;
    }
    public void setWebsite(String website) {
        this.website = website;
    }
    public void setDateJoined(long dateJoined) {
        this.dateJoined = dateJoined;
    }
}