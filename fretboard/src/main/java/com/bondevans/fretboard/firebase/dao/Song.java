package com.bondevans.fretboard.firebase.dao;

/**
 * Simple Song Class for use with Firebase for retreiving and storing song details
 */
public class Song {
    String id;
    String name;
    String description;

    /**
     * Empty constructir
     */
    @SuppressWarnings("unused")
    public Song(){}

    public Song(String id, String name, String description){
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}