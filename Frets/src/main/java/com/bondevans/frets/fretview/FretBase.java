package com.bondevans.frets.fretview;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for FretSong/FretTrack/FretEvent/FretNote
 */
class FretBase {

//    private static final String TAG = FretBase.class.getSimpleName();
    static final String ATTR_MIDI_INSTRUMENT = "mi";
    static final String ATTR_FRET_INSTRUMENT = "fi";
    static final String ATTR_DELTATICKS = "dt";
    static final String ATTR_TEMPO = "te";
    static final String ATTR_BEND = "be";
    static final String ATTR_EV_TRACK = "et";
    static final String ATTR_EV_TOTALTICKS = "tt";
    static final String ATTR_EV_CLICKEVENT = "ce";
    static final String ATTR_EV_HASONNOTES = "ho";
    static final String ATTR_NAME = "na";
    static final String ATTR_TPQN = "tpqn";
    static final String ATTR_BPM = "bpm";
    static final String ATTR_SOLO = "so";
    static final String ATTR_NOTE = "no";
    static final String ATTR_ON = "on";
    static final String ATTR_STRING = "st";
    static final String ATTR_FRET = "fr";
    static final String ATTR_KEYW = "ke";
    static final String ATTR_DRUM_TRACK = "dr";
    static final String ATTR_CLICK_TRACK = "ct";
    static final String ATTR_CLICK_TRACKSIZE = "cts";
    static final String ATTR_MERGED = "me";


    /**
     * Constructor
     */
    FretBase() {
    }

    protected String attr(String name, boolean value) {
        return name + "=" + "\"" + (value ? "1" : "0") + "\" ";
    }
    protected String attr(String name, int value) {

        return name + "=" + "\"" + value+"\" ";
    }
    protected String attr(String name, String value) {
        return name + "=" + "\""+value+"\" ";
    }
    static String getTagString(String ev, String tag){
        String ret="";
        Pattern tagPattern = Pattern.compile(tag+"=\"(.*?)[\"<]",
                Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
        Matcher m = tagPattern.matcher(ev);
        if (m.find()) { // Find each match in turn;
            ret = m.group(1); // Access a submatch group;
        }
        return ret;
    }
    static int getTagInt(String ev, String tag){
        int ret;
        try {
            ret = Integer.decode(getTagString(ev, tag));
        }
        catch (Exception e){
            ret = 0;
        }
        return ret;
    }

}
