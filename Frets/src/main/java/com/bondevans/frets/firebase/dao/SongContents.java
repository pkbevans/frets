package com.bondevans.frets.firebase.dao;

/**
 * Simple Songs Class for use with Firebase for retrieving and storing song details
 */
public class SongContents {
    public static String childName = SongContents.class.getSimpleName().toLowerCase();
    String contents;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public SongContents(){}

    public SongContents(String contents){
        this.contents = contents;
    }

    public String getContents() {
        return contents;
    }
}