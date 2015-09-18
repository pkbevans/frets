package com.bondevans.fretboard.fretboardplayer;

import java.util.List;

/**
 * A FretEvent is a collection of notes all played at the same time
 */
public class FretEvent {
    long    timeDelay;
    List<FretNote> fretNotes;

    /**
     * Constructor
     * @param timeDelay
     * @param fretNotes
     */
    public FretEvent(long timeDelay, List<FretNote> fretNotes){
        this.timeDelay = timeDelay;
        this.fretNotes = fretNotes;
    }
}
