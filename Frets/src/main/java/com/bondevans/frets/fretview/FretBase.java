package com.bondevans.frets.fretview;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for FretSong/FretTrack/FretEvent/FretNote
 */
class FretBase {

//    private static final String TAG = FretBase.class.getSimpleName();
    static final String JSON_TRACKS = "tracks";
    static final String JSON_EVENTS = "events";
    static final String JSON_CLICK_EVENTS = "clickEvents";
    static final String JSON_NOTES = "notes";
    static final String JSON_PLAYER_EVENTS = "playerEvents";
    static final String JSON_UI_NOTES = "uiNotes";

    /**
     * Constructor
     */
    FretBase() {
    }
}
