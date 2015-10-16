package com.bondevans.fretboard.fretboardplayer;

/**
 * NoteEvent describes a midi NOTE ON or NOTE OFF event
 */
public class NoteEvent {
    public final static int TYPE_NOTE = 1;
    public final static int TYPE_TEMPO = 2;

    int     type;
    int     note;
    boolean on;         // false=off, true=on,
    int     deltaTime;  // Time delay in mTicks before firing this event
    int     tempo;      // tempo change event

    public NoteEvent(int note, boolean on, int deltaTime) {
        this.type = TYPE_NOTE;
        this.note = note;
        this.on = on;
        this.deltaTime = deltaTime;
    }
    public NoteEvent(int tempo, int deltaTime) {
        this.type = TYPE_TEMPO;
        this.tempo = tempo;
        this.deltaTime = deltaTime;
    }
}