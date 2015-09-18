package com.bondevans.fretboard.fretboardplayer;

/**
 * Created by Paul on 8/7/2015.
 */
public class NoteEvent {
    int     note;
    boolean on;         // false=off, true=on,
    int     deltaTime;  // Time delay in mTicks before firing this event

    public NoteEvent(int note, boolean on, int deltaTime) {
        this.note = note;
        this.on = on;
        this.deltaTime = deltaTime;
    }
}