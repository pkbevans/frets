package com.bondevans.fretboard.fretboardplayer;

/**
 * Created by Paul on 8/7/2015.
 */
public class FretEvent {
    int     note;
    int     type;         // 0=off, 1=on,
    int     deltaTime;  // Time delay in ticks before firing this event

    public FretEvent(int note, int type, int deltaTime) {
        this.note = note;
        this.type = type;
        this.deltaTime = deltaTime;
    }
}