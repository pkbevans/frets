package com.bondevans.fretboard.fretview;

import com.bondevans.fretboard.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of Fretevents making up a track
 */
public class FretTrack extends FretBase {
    private static final String TAG = FretTrack.class.getSimpleName();
    public static final String ELEMENT_TRACK = "track";
    public static final String TRACK_ELEMENT_OPEN = "<"+ELEMENT_TRACK+">";
    public static final String TRACK_ELEMENT_CLOSE = "</"+ELEMENT_TRACK+">";
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "na";
    private static final String ATTR_INSTRUMENT = "in";
    String id;
    String name;
    String instrument;
    List<FretEvent> fretEvents;

    /**
     * Constructor
     * @param id Unique ID for this track
     * @param name Song name
     * @param instrument Instrument
     * @param fretEvents List of fret events
     */
    public FretTrack(String id, String name, String instrument, List<FretEvent> fretEvents){
        this.id = id;
        this.name = name;
        this.instrument = instrument;
        this.fretEvents = fretEvents;
    }

    public FretTrack(String track) {
        Log.d(TAG, "ev=[" + track + "]");
        fretEvents = new ArrayList<>();
        this.id = getTagString(track, ATTR_ID);
        this.name = getTagString(track, ATTR_NAME);
        this.instrument = getTagString(track, ATTR_INSTRUMENT);
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
            Log.d(TAG, "HELLO found <ev> tag: [" + ev + "]");
            fretEvents.add(new FretEvent(ev));
        }
    }

    /**
     * Output contents to XML - used to serialize class
     * @return String representation of class
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(TRACK_ELEMENT_OPEN
                +attr(ATTR_ID, id)
                +attr(ATTR_NAME, name)
                +attr(ATTR_INSTRUMENT, instrument));
        for(FretEvent event: fretEvents){
            sb.append(event.toString());
        }
        sb.append(TRACK_ELEMENT_CLOSE);
        return sb.toString();
    }
}

