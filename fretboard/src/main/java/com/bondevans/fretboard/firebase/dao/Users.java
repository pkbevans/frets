package com.bondevans.fretboard.firebase.dao;

/**
 * Simple Class for use with Firebase for storing user details
 */
public class Users {
    public static String childName = Users.class.getSimpleName().toLowerCase();
    String username;
    String email;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public Users() {
    }

    public Users(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}