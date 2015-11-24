package com.bondevans.fretboard.firebase.dao;

import java.util.Calendar;

/**
 * Records usage of the app by a given user
 */
public class Usage {
    public static final String FEATURE_BROWSETO_FILE = "btf";
    private String dateTime;
    private String feature;

    public Usage(){}

    public Usage(String feature){
        Calendar c = Calendar.getInstance();
        this.dateTime = (c.get(Calendar.YEAR)*10000)+(c.get(Calendar.MONTH)*100)+c.get(Calendar.DAY_OF_MONTH)+"-"+c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
        this.feature = feature;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getFeature() {
        return feature;
    }


}
