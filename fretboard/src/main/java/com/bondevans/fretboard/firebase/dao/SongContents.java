package com.bondevans.fretboard.firebase.dao;

/**
 * Simple Songs Class for use with Firebase for retreiving and storing song details
 */
public class SongContents {
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