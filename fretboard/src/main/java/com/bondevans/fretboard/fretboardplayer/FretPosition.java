package com.bondevans.fretboard.fretboardplayer;

import android.util.Log;

import java.util.List;

public class FretPosition {

    private static final String TAG = "FretPosition";
    private int numStrings;    // Nunmber of numStrings on this instrument
    private int numFrets;      // Number of numFrets on this instrument
    private int[] tuning;  // Tuning - e.g. on guitar EADGBE = 40, 45, 50, 55, 59, 64
    private String [] stringNames;  // For Debugging purposes mainly.

    private boolean[] stringAvailable;

    /**
     * Constructor sets up instrument
     */
    public FretPosition(int numStrings, int numFrets, int[] tuning, String [] stringNames) {
        Log.d(TAG, "Constructor");
        this.numStrings = numStrings;
        this.numFrets = numFrets;
        this.tuning = tuning;
        this.stringNames = stringNames;
        // Initialise arrays
        this.stringAvailable = new boolean[numStrings];
    }

    public List<FretNote> getFretPositions(List<FretNote> fretNotes) {
        if(fretNotes == null){
            return null;
        }
        List<FretNote> mFretNotes;
        Log.d(TAG, "getFretPositions");
        mFretNotes = fretNotes;
        // reset stringAvailable
        for (int i = 0; i < numStrings; i++) {
            stringAvailable[i] = true;
        }
        // Work out string/fret positions for each note
        FretNote fretNote;
        boolean found = false;
        // Loop round each note in the array - starting with the last (assume it is the highest) TODO - sort into high-to-low order
        int i = mFretNotes.size() - 1;
        while (i >= 0) {
            fretNote = mFretNotes.get(i);
            // Utterly simplistic - get first position that fits
            // Start with highest note and top string
            for(int j = 0;j < numStrings; j++) {
                if (stringAvailable[j]) {
                    if (fretNote.note >= tuning[j] && fretNote.note <= (tuning[j] + numFrets)) {
                        fretNote.string = j;
                        fretNote.fret = fretNote.note - tuning[j];
                        fretNote.name = setName(fretNote.note);
                        if(fretNote.on) {
                            // Only use up a string for ON notes
                            stringAvailable[j] = false;
                        }
                        Log.d(TAG, "Found position for note[" + i + "] [" + fretNote.note + "] string[" +
                                stringNames[fretNote.string] + "] fret [" + fretNote.fret +
                                "] name [" + fretNote.name + "]");
                        // Update FretNote list with updated element
                        mFretNotes.set(i,fretNote);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                // If we get here something went wrong!!
                Log.d(TAG, "OOPS - Can find position for note: [" + fretNote.note + "]");
            }
            found = false;
            --i;
        }
        return mFretNotes;
    }

    private final static String[] noteName = {"C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "B", "Bb"};

    private String setName(int note) {
        int div = note / 12;
        int mod = note % 12;
        return noteName[mod] + (div - 1);
    }
}
