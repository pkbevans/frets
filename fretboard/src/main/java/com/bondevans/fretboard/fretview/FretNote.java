package com.bondevans.fretboard.fretview;

import com.bondevans.fretboard.midi.MidiFile;
import com.bondevans.fretboard.utils.Log;

/**
 * A note on the fretboard
 */
public class FretNote extends FretBase {
    private static final String TAG = FretNote.class.getSimpleName();
    public static final String ELEMENT_NOTE = "nt";
    public static final String NOTE_ELEMENT_OPEN = "<" + ELEMENT_NOTE + ">";
    public static final String NOTE_ELEMENT_CLOSE = "</" + ELEMENT_NOTE + ">";
    public static final int MAX_BEND = 10;
    private static final int NOT_SET = 99;
    private static final String ATTR_NOTE = "no";
    private static final String ATTR_ON = "on";
    private static final String ATTR_STRING = "st";
    private static final String ATTR_FRET = "fr";
    private static final String ATTR_BEND = "be";
    private static final String ATTR_NAME = "na";
    private static final int ZERO_PITCH_BEND = 8192;
    int note;               // Midi note value (Bottom E on standard Guitar is 40)
    public boolean on;      // True = turn note ON, False = turn FretNote off
    int string;             // String number from 0 to mStrings (0=highest string - i.e. Top E on standard guitar)
    int fret;               // fret position
    String name;            // FretNote name (e.g. E, F#, etc)
    int bend;               // Amount of bend on the string.  A value between 0-10

    /**
     * Constructor takes note, ON/OFF and fret details
     *
     * @param note   Note midi value
     * @param on     Note on (true) or off (false)
     * @param string String to play note on
     * @param fret   Fret to play note on
     * @param name   Name of the note
     */
    public FretNote(int note, boolean on, int string, int fret, String name) {
        this.note = note;
        this.on = on;
        this.string = string;
        this.fret = fret;
        this.name = name;
    }

    /**
     * Constructor sets note and ON/OFF
     *
     * @param note Midi note value
     * @param on true = on false = off
     */
    public FretNote(int note, boolean on) {
        this.note = note;
        this.on = on;
        this.string = NOT_SET;
        this.fret = NOT_SET;
    }

    public FretNote(String note) {
        this.note = getTagInt(note, ATTR_NOTE);
        this.string = getTagInt(note, ATTR_STRING);
        this.fret = getTagInt(note, ATTR_FRET);
        this.bend = getTagInt(note, ATTR_BEND);
        this.name = MidiFile.noteName(this.note);
        String on = getTagString(note, ATTR_ON);
        this.on = on.equalsIgnoreCase("1");
    }

    /**
     * Sets Bend value in range given a midi pitch bend amount in the range 0 - 16383
     * 8192 = ZERO.  Anything below this is ignored - you can't bend down.
     *
     * @param bendValue
     */
    public void setBend(int bendValue) {
//        this.bend = (bendValue-ZERO_PITCH_BEND<0)?0:(bendValue-MAX_PITCH_BEND)/MAX_PITCH_BEND/10;
        this.bend = bendValue > ZERO_PITCH_BEND ? (bendValue - ZERO_PITCH_BEND) / (ZERO_PITCH_BEND / MAX_BEND) : 0;

        Log.d(TAG, "HELLO setBend:" + bendValue + "=>" + this.bend);
    }

    @Override
    public String toString() {
        return NOTE_ELEMENT_OPEN +
                attr(ATTR_NOTE, note) +
                attr(ATTR_ON, on) +
                attr(ATTR_STRING, string) +
                attr(ATTR_FRET, fret) +
                attr(ATTR_BEND, bend) +
//                attr(ATTR_NAME, name) +
                NOTE_ELEMENT_CLOSE;
    }
}
