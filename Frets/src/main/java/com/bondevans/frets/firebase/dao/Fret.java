package com.bondevans.frets.firebase.dao;
@SuppressWarnings("unused")

public class Fret {
    public static String childName = Fret.class.getSimpleName().toLowerCase();
    public static int LEAD_GUITAR = 0;
    public static int RYTHM_GUITAR = 1;
    public static int BASS = 2;
    public static int UKELELE = 3;
    private String [] instrumentName={
            "LEAD GUITAR",
            "RYTHM GUITAR",
            "BASS",
            "UKELELE"
    };    String  id;
    String  songName;
    String  description;
    String  userId;
    int     instrument;
    long    datePublished;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    public Fret(){}

    public Fret(String id, String songName, String description, String userId, int instrument, long datePublished){
        this.id = id;
        this.songName = songName;
        this.description = description;
        this.userId = userId;
        this.instrument = instrument;
        this.datePublished = datePublished;
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
    public String getUserId() {
        return userId;
    }
    public int getInstrument() {
        return instrument;
    }
    public long getDatePublished() {
        return datePublished;
    }
    public String getInstrumentName(int index) {
        return instrumentName[index];
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
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setDatePublished(long datePublished) {
        this.datePublished = datePublished;
    }
    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }
}