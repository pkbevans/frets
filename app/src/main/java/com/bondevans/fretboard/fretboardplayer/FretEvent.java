package com.bondevans.fretboard.fretboardplayer;

import java.util.List;

/**
 * A FretEvent is an event that impacts on the Fretboard
 */
public class FretEvent {
    int deltaTime;
    List<FretNote> fretNotes;
    int tempo;

    /**
     * Constructor
     * @param deltaTime
     * @param fretNotes
     */
    public FretEvent(int deltaTime, List<FretNote> fretNotes, int tempo){
        this.deltaTime = deltaTime;
        this.fretNotes = fretNotes;
        this.tempo = tempo;
    }
}
