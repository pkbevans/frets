package com.bondevans.frets.midi;

import com.bondevans.frets.utils.Log;

/**
 * MidiNoteEvent describes a midi NOTE ON or NOTE OFF event
 * TODO - need to split this out into base and subclasses for different uses
 */
public class MidiNoteEvent {
    public final static int TYPE_NOTE = 1;
    public final static int TYPE_TEMPO = 2;
    public final static int TYPE_BEND = 3;
    private static final String TAG = MidiNoteEvent.class.getSimpleName();

    int     type;
    int     note;
    boolean on;         // false=off, true=on,
    int     deltaTime;  // Time delay in mTicks before firing this event
    int     tempo;      // instrument change event
    int bend;

    public MidiNoteEvent(int note, boolean on, int deltaTime) {
        this.type = TYPE_NOTE;
        this.note = note;
        this.on = on;
        this.deltaTime = deltaTime;
    }

    public MidiNoteEvent(int type, int deltaTime, int value) {
        this.type = type;
        this.deltaTime = deltaTime;
        switch (type) {
            case TYPE_BEND:
                this.bend = value;
                break;
            case TYPE_TEMPO:
                this.tempo = value;
                break;
            case TYPE_NOTE:
                // SHOULD NEVER HAPPEN!!!!
                break;
        }
    }
}