package com.bondevans.fretboard.fretview;

import android.util.Log;

import com.bondevans.fretboard.instruments.FretInstrument;

import java.util.List;

public class FretPosition {

    private static final String TAG = "FretPosition";
    private int mNumStrings;    // Nunmber of strings on this instrument
    private int mNumFrets;      // Number of frets on this instrument
    private int[] mTuning;  // Tuning - e.g. on guitar EADGBE = 40, 45, 50, 55, 59, 64
    private String[] mStringNames;  // For Debugging purposes mainly.
    private boolean[] mStringAvailable;

    /**
     * Constructor takes FretInstrument definition
     * @param instrument Instrument definition
     */
    public FretPosition(FretInstrument.Instrument instrument) {
        Log.d(TAG, "Constructor");
        this.mNumStrings = instrument.numStrings();
        this.mNumFrets = instrument.numFrets();
        this.mTuning = instrument.TUNING();
        this.mStringNames = instrument.STRING_NAMES();
        // Initialise arrays
        this.mStringAvailable = new boolean[mNumStrings];
    }

    /**
     * Updates list of <code>FretNote</code>s with Fret positions (if found)
     *
     * @param fretNotes
     * @return
     */
    public List<FretNote> getFretPositions(List<FretNote> fretNotes) {
        if(fretNotes == null){
            return null;
        }
        List<FretNote> mFretNotes;
//        Log.d(TAG, "getFretPositions");
        mFretNotes = fretNotes;
        // reset mStringAvailable
        for (int i = 0; i < mNumStrings; i++) {
            mStringAvailable[i] = true;
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
            for (int j = 0; j < mNumStrings; j++) {
                if (mStringAvailable[j]) {
                    if (fretNote.note >= mTuning[j] && fretNote.note <= (mTuning[j] + mNumFrets)) {
                        fretNote.string = j;
                        fretNote.fret = fretNote.note - mTuning[j];
                        fretNote.name = setName(fretNote.note);
                        if(fretNote.on) {
                            // Only use up a string for ON notes
                            mStringAvailable[j] = false;
                        }
//                        Log.d(TAG, "Found position for note[" + i + "] [" + fretNote.note + "] string[" +
//                                mStringNames[fretNote.string] + "] fret [" + fretNote.fret +
//                                "] name [" + fretNote.name + "]");
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

    /**
     * Takes a <code>FretNote</code> and returns the same note moved to the next string
     *
     * @param fretNote the note
     * @param up       true = UP, false = DOWN
     */
    public void moveNoteOneString(FretNote fretNote, boolean up) {
        if ((up && fretNote.string >= (mNumStrings - 1)) || (!up && fretNote.string == 0)) {
            // Already on lowest string so cant move up the fret board
            // or already on highest string so cant move down the fretboard
            Log.d(TAG, "moveNoteOneString1");
            return;
        }
        if (!up && (fretNote.note - mTuning[fretNote.string - 1]) < 0) {
            // Already too close to the nut to move down the fretboard
            Log.d(TAG, "moveNoteOneString2");
            return;
        }
        if (up && mTuning[fretNote.string + 1] - fretNote.note >= mNumFrets) {
            // Already too close to the bridge to move up the fretboard
            Log.d(TAG, "moveNoteOneString3");
            return;
        }

        fretNote.string += (up ? 1 : -1);
        // find new fret position...
        fretNote.fret = fretNote.note - mTuning[fretNote.string];
    }
}
