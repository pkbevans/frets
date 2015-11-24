package com.bondevans.fretboard.fretview;

import com.bondevans.fretboard.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of <class>FretTrack</class>s making up a track
 */
public class FretSong extends FretBase {
    private static final String TAG = FretSong.class.getSimpleName();
    public static final String ELEMENT_SONG = "song";
    public static final String SONG_ELEMENT_OPEN = "<"+ELEMENT_SONG+">";
    public static final String SONG_ELEMENT_CLOSE = "</"+ELEMENT_SONG+">";
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "na";
    String id;
    String name;
    List<FretTrack> fretTracks;

    /**
     * Constructor
     * @param id Unique ID for this track
     * @param name Song name
     * @param fretTracks List of fret events
     */
    public FretSong(String id, String name, List<FretTrack> fretTracks){
        this.id = id;
        this.name = name;
        this.fretTracks = fretTracks;
    }

    public FretSong(String song) {
        Log.d(TAG, "ev=[" + song + "]");
        fretTracks = new ArrayList<>();
        this.id = getTagString(song, ATTR_ID);
        this.name = getTagString(song, ATTR_NAME);
        loadFretTracks(song);
    }

    private static Pattern trackPattern = Pattern.compile("<track[^>]*>(.*?)</track>",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    private void loadFretTracks(String song) {
        fretTracks= new ArrayList<>();
        Matcher matcher = trackPattern.matcher(song);

        // look for contents of <ev></ev>
        while (matcher.find()) {
            String track = matcher.group(1);
            Log.d(TAG, "HELLO found "+FretTrack.ELEMENT_TRACK+" tag: [" + track + "]");
            fretTracks.add(new FretTrack(track));
        }
    }

    /**
     * Output contents to XML - used to serialize class
     * @return String representation of class
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(SONG_ELEMENT_OPEN
                +attr(ATTR_ID, id)
                +attr(ATTR_NAME, name));
        for(FretTrack track: fretTracks){
            sb.append(track.toString());
        }
        sb.append(SONG_ELEMENT_CLOSE);
        return sb.toString();
    }
}

