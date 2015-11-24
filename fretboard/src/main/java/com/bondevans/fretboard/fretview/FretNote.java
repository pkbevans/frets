package com.bondevans.fretboard.fretview;

/**
 * A note on the fretboard
 */
public class FretNote extends FretBase {
    public static final String ELEMENT_NOTE = "note";
    public static final String NOTE_ELEMENT_OPEN = "<" + ELEMENT_NOTE + ">";
    public static final String NOTE_ELEMENT_CLOSE = "</" + ELEMENT_NOTE + ">";
    private static final int NOT_SET = 99;
    private static final String ATTR_NOTE = "no";
    private static final String ATTR_ON = "on";
    private static final String ATTR_STRING = "st";
    private static final String ATTR_FRET = "fr";
    private static final String ATTR_NAME = "na";
    int note;   // Midi note value (Bottom E on standard Guitar is 40)
    boolean on;     // True = turn note ON, False = turn FretNote off
    int string; // String number from 0 to mStrings (0=highest string - i.e. Top E on standard guitar)
    int fret;   // fret position
    String name;   // FretNote name (e.g. E, F#, etc)

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
        this.name = getTagString(note, ATTR_NAME);
        String on = getTagString(note, ATTR_ON);
        this.on = on.equalsIgnoreCase("true");
    }

    @Override
    public String toString() {
        return NOTE_ELEMENT_OPEN +
                attr(ATTR_NOTE, note) +
                attr(ATTR_ON, on) +
                attr(ATTR_STRING, string) +
                attr(ATTR_FRET, fret) +
                attr(ATTR_NAME, name) +
                NOTE_ELEMENT_CLOSE;
    }
}
