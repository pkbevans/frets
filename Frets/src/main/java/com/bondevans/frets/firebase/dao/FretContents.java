package com.bondevans.frets.firebase.dao;

/**
 * Simple Class for use with Firebase for retrieving and storing song contents
 */
public class FretContents {
    public static String childName = FretContents.class.getSimpleName().toLowerCase();
    String contents;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public FretContents(){}

    public FretContents(String contents){
        this.contents = contents;
    }

    public String getContents() {
        return contents;
    }
}