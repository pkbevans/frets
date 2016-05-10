package com.bondevans.fretboard.fretview;

//import com.bondevans.fretboard.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of Fretevents making up a track
 */
public class FretTrack extends FretBase {
    //    private static final String TAG = FretTrack.class.getSimpleName();
    public static final String ELEMENT_TRACK = "track";
    public static final String TRACK_ELEMENT_OPEN = "<"+ELEMENT_TRACK+">";
    public static final String TRACK_ELEMENT_CLOSE = "</"+ELEMENT_TRACK+">";
    private static final String ATTR_NAME = "na";
    private String name;
    public List<FretEvent> fretEvents;

    /**
     * Constructor
     * @param name Songs name
     * @param fretEvents List of fret events
     */
    public FretTrack(String name, List<FretEvent> fretEvents){
        this.name = name;
        this.fretEvents = fretEvents;
    }

    /**
     * Constructor takes an XML-like string containing a complete track
     * @param track XML representation of the class
     */
    public FretTrack(String track) {
//        Log.d(TAG, "track=[" + track + "]");
        fretEvents = new ArrayList<>();
        this.name = getTagString(track, ATTR_NAME);
        loadFretEvents(track);
    }

    private static Pattern evPattern = Pattern.compile("<ev[^>]*>(.*?)</ev>",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    private void loadFretEvents(String track) {
        fretEvents = new ArrayList<>();
        Matcher matcher = evPattern.matcher(track);

        // look for contents of <ev></ev>
        while (matcher.find()) {
            String ev = matcher.group(1);
//            Log.d(TAG, "HELLO found <ev> tag: [" + ev + "]");
            fretEvents.add(new FretEvent(ev));
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Output contents to XML - used to serialize class
     * @return String representation of class
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(TRACK_ELEMENT_OPEN
                + attr(ATTR_NAME, name)
        );
        for (FretEvent event : fretEvents) {
            sb.append(event.toString());
        }
        sb.append(TRACK_ELEMENT_CLOSE);
        return sb.toString();
    }

    public void setName(String name) {
        this.name = name;
    }
}

