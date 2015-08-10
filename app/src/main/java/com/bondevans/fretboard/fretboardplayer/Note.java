package com.bondevans.fretboard.fretboardplayer;

/**
 * A note on the fretboard
 */
public class Note {
    int     string; // String number from 0 to mStrings (0=highest string - i.e. Top E on standard guitar)
    int     note;   // Midi note value (Bottom e on standard Guitar is 40)
    int     fret;   // fret position
    boolean on;     // True = turn note ON, False = turn Note off
    String  name;   // Note name (e.g. E, F#, etc)

    public Note(int string, int note, int fret, boolean on, String name) {
        this.string = string;
        this.note = note;
        this.fret= fret;
        this.on = on;
        this.name = name;
    }
}
