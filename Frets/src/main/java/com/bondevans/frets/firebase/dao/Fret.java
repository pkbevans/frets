package com.bondevans.frets.firebase.dao;

import com.bondevans.frets.instruments.Instrument;

@SuppressWarnings("unused")

public class Fret {
    public static String childName = Fret.class.getSimpleName().toLowerCase();
    String  contentId;
    String  songName;   // Stored as UPPERCASE to help Search capability and to create conformity
    String  description;
    String  userId;
    String  userName;
    int     instrument;
    long    datePublished;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public Fret(){}

    public Fret(String contentId, String songName, String description, String userId, String userName, int instrument, long datePublished){
        this.contentId = contentId;
        this.songName = songName.toUpperCase();
        this.description = description;
        this.userId = userId;
        this.userName = userName;
        this.instrument = instrument;
        this.datePublished = datePublished;
    }

    public String getName() {
        return songName;
    }
    public String getDescription() {
        return description;
    }
    public String getContentId() {
        return contentId;
    }
    public String getUserId() {
        return userId;
    }
    public String getUserName() {
        return userName;
    }
    public int getInstrument() {
        return instrument;
    }
    public long getDatePublished() {
        return datePublished;
    }
    public String getInstrumentName(int index) {
        return Instrument.values()[index].getDescription();
    }
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
    public void setName(String songName) {
        this.songName = songName.toUpperCase();
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public void setDatePublished(long datePublished) {
        this.datePublished = datePublished;
    }
    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }
}