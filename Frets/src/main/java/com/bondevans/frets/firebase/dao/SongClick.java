package com.bondevans.frets.firebase.dao;

import java.util.Calendar;

/**
 * Records song click by user
 */
public class SongClick {
    public static String childName = SongClick.class.getSimpleName().toLowerCase();
    private String dateTime;
    private String songId;
    private String uId;

    public SongClick() {
    }

    public SongClick(String songId, String uId) {
        Calendar c = Calendar.getInstance();
        this.dateTime = (c.get(Calendar.YEAR)*10000)+(c.get(Calendar.MONTH)*100)+c.get(Calendar.DAY_OF_MONTH)+"-"+c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
        this.songId = songId;
        this.uId = uId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getSongId() {
        return songId;
    }

    public String getUId() {
        return uId;
    }
}
