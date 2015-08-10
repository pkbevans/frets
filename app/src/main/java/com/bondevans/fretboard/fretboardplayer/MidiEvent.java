package com.bondevans.fretboard.fretboardplayer;

/**
 * Created by Paul on 8/7/2015.
 */
public class MidiEvent {
    int     note;
    int     type;         // 0=off, 1=on,
    int     deltaTime;  // Time delay since previous event

    public MidiEvent(int note, int type, int deltaTime) {
        this.note = note;
        this.type = type;
        this.deltaTime = deltaTime;
    }
}