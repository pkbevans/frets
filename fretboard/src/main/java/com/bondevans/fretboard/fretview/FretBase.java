package com.bondevans.fretboard.fretview;

import com.bondevans.fretboard.utils.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for FretSong/FretTrack/FretEvent/FretNote
 */
public class FretBase {

    private static final String TAG = FretBase.class.getSimpleName();

    /**
     * Constructor
     */
    public FretBase() {
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
    protected static String getTagString(String ev, String tag){
        String ret="";
        Pattern tagPattern = Pattern.compile(tag+"=\"(.*?)[\"<]",
                Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
        Matcher m = tagPattern.matcher(ev);
        if (m.find()) { // Find each match in turn;
            ret = m.group(1); // Access a submatch group;
        }
        return ret;
    }
    protected static int getTagInt(String ev, String tag){
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
