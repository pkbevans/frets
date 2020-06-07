package com.bondevans.frets.firebase.dao;

import java.util.Calendar;

/**
 * Records song click by user
 */
public class FretClick {
    public static String childName = FretClick.class.getSimpleName().toLowerCase();
    private String dateTime;
    private String fretId;
    private String uId;

    public FretClick() {
    }

    public FretClick(String fretId, String uId) {
        Calendar c = Calendar.getInstance();
        this.dateTime = (c.get(Calendar.YEAR)*10000)+(c.get(Calendar.MONTH)*100)+c.get(Calendar.DAY_OF_MONTH)+"-"+c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
        this.fretId = fretId;
        this.uId = uId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getFretId() {
        return fretId;
    }

    public String getUId() {
        return uId;
    }
}
