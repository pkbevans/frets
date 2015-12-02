package com.bondevans.fretboard.firebase.dao;

/**
 * Simple Songs Class for use with Firebase for retreiving and storing song details
 */
public class Songs {
    String id;
    String name;
    String description;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public Songs(){}

    public Songs(String id, String name, String description){
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    public String getId() {
        return id;
    }
}