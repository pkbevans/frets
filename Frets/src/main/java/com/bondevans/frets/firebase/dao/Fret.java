package com.bondevans.frets.firebase.dao;

/**
 * Simple Class for use with Firebase for retrieving and storing Fret details
 */
public class Fret {
    public static String childName = Fret.class.getSimpleName().toLowerCase();
    String id;
    String songName;
    String description;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public Fret(){}

    public Fret(String id, String songName, String description){
        this.id = id;
        this.songName = songName;
        this.description = description;
    }

    public String getName() {
        return songName;
    }
    public String getDescription() {
        return description;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public void setName(String songName) {
        this.songName = songName;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}