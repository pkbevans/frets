package com.bondevans.fretboard.fretboardplayer;

import java.util.List;

/**
 * A FretEvent is a collection of notes all played at the same time
 */
public class FretEvent {
    int deltaTime;
    List<FretNote> fretNotes;

    /**
     * Constructor
     * @param deltaTime
     * @param fretNotes
     */
    public FretEvent(int deltaTime, List<FretNote> fretNotes){
        this.deltaTime = deltaTime;
        this.fretNotes = fretNotes;
    }
}
