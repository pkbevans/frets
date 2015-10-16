package com.bondevans.fretboard.fretboardplayer;

/**
 * A note on the fretboard
 */
public class FretNote {
    private static final int NOT_SET = 99;
    int     note;   // Midi note value (Bottom E on standard Guitar is 40)
    boolean on;     // True = turn note ON, False = turn FretNote off
    int     string; // String number from 0 to mStrings (0=highest string - i.e. Top E on standard guitar)
    int     fret;   // fret position
    String  name;   // FretNote name (e.g. E, F#, etc)

    /**
     * Constructor takes note, ON/OFF and fret details
     * @param note
     * @param on
     * @param string
     * @param fret
     * @param name
     */
    public FretNote(int note, boolean on, int string, int fret, String name) {
        this.note = note;
        this.on = on;
        this.string = string;
        this.fret= fret;
        this.name = name;
    }

    /**
     * Constructor sets note and ON/OFF
     * @param note
     * @param on
     */
    public FretNote(int note, boolean on) {
        this.note = note;
        this.on = on;
        this.string = NOT_SET;
        this.fret = NOT_SET;
    }
}
