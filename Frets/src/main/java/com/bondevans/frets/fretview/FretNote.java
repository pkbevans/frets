package com.bondevans.frets.fretview;

import com.bondevans.frets.midi.MidiFile;

/**
 * A note on the fretboard
 */
public class FretNote extends FretBase {
    private static final String TAG = FretNote.class.getSimpleName();
    public static final String ELEMENT_NOTE = "nt";
    public static final String NOTE_ELEMENT_OPEN = "<" + ELEMENT_NOTE + ">";
    public static final String NOTE_ELEMENT_CLOSE = "</" + ELEMENT_NOTE + ">";
    private static final int NOT_SET = 99;
    int note;               // Midi note value (Bottom E on standard Guitar is 40)
    public boolean on;      // True = turn note ON, False = turn FretNote off
    int string;             // String number from 0 to mStrings (0=highest string - i.e. Top E on standard guitar)
    int fret;               // fret position
    int bend;               // bend value applied to this note
    String name;            // FretNote name (e.g. E, F#, etc)

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
        this.name = MidiFile.noteName(this.note);
        String on = getTagString(note, ATTR_ON);
        this.on = on.equalsIgnoreCase("1");
    }

    @Override
    public String toString() {
        return NOTE_ELEMENT_OPEN +
                attr(ATTR_NOTE, note) +
                attr(ATTR_ON, on) +
                attr(ATTR_STRING, string) +
                attr(ATTR_FRET, fret) +
//                attr(ATTR_NAME, name) +
                NOTE_ELEMENT_CLOSE;
    }
}
