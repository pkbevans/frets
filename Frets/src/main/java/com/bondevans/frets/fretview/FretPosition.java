package com.bondevans.frets.fretview;

import android.util.Log;

import com.bondevans.frets.instruments.FretBassGuitarStandard;
import com.bondevans.frets.instruments.FretGuitarStandard;
import com.bondevans.frets.instruments.FretInstrument;

import java.util.Collections;
import java.util.List;

public class FretPosition {

    private static final String TAG = "FretPosition";
    private int mNumStrings;    // Nunmber of strings on this instrument
    private int mNumFrets;      // Number of frets on this instrument
    private int[] mTuning;  // Tuning - e.g. on guitar EADGBE = 40, 45, 50, 55, 59, 64
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
        // Initialise arrays
        this.mStringAvailable = new boolean[mNumStrings];
    }

    public FretPosition(int fretInstrument) {
        FretInstrument.Instrument instrument;
        switch(fretInstrument){
            case 0:
                instrument = new FretGuitarStandard();
                break;
            case 1:
                instrument = new FretBassGuitarStandard();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + fretInstrument);
        }
        this.mNumStrings = instrument.numStrings();
        this.mNumFrets = instrument.numFrets();
        this.mTuning = instrument.TUNING();
        // Initialise arrays
        this.mStringAvailable = new boolean[mNumStrings];
    }

    /**
     * Updates list of <code>FretNote</code>s with Fret positions (if found)
     *
     * @param fretNotes list of fret notes
     * @return updated list of fret notes
     */
    public List<FretNote> getFretPositions(List<FretNote> fretNotes) {
        if(fretNotes == null){
            return null;
        }
        Log.d(TAG, "HELLO Notes:"+fretNotes.size());
        // reset mStringAvailable
        for (int i = 0; i < mNumStrings; i++) {
            mStringAvailable[i] = true;
        }
        // Work out string/fret positions for each note
        // Loop round each note in the array - starting with the last (assume it is the highest)
        Collections.sort(fretNotes, Collections.reverseOrder());
        for(FretNote fretNote: fretNotes) {
            // Utterly simplistic - get first position that fits
            // Start with highest note and top string
            for (int j = 0; j < mNumStrings; j++) {
                if (mStringAvailable[j]) {
                    if (fretNote.note >= mTuning[j] && fretNote.note <= (mTuning[j] + mNumFrets)) {
                        fretNote.string = j;
                        fretNote.fret = fretNote.note - mTuning[j];
                        fretNote.name = setName(fretNote.note);
                        if(fretNote.on){Log.d(TAG, "HELLO Note:"+fretNote.toString());}
                        if(fretNote.on) {
                            // Only use up a string for ON notes
                            mStringAvailable[j] = false;
                        }
                        break;
                    }
                }
            }
        }
        return fretNotes;
    }

    /**
     * Get the <code>FretPostion</code> closest to the specified fret
     * @param fretNotes <code>FretNote</code> list
     * @param targetFret Target fret
     * @return Updated <code>FretNote</code> list
     */
    public List<FretNote> getFretPositions(List<FretNote> fretNotes, int targetFret) {
        if(fretNotes == null){
            return null;
        }
        //  Don't bother for multiple notes (yet) - Too difficult
        if(fretNotes.size()>1){
            return fretNotes;
        }
        // reset mStringAvailable
        for (int i = 0; i < mNumStrings; i++) {
            mStringAvailable[i] = true;
        }
        // Work out string/fret positions for each note
        // Loop round each note in the array - starting with the last (assume it is the highest)
        Collections.sort(fretNotes, Collections.reverseOrder());
        for(FretNote fretNote: fretNotes) {
            // Start with highest note and top string
            for (int j = 0; j < mNumStrings; j++) {
                if (mStringAvailable[j]) {
                    // String is available
                    if (fretNote.note >= mTuning[j] && fretNote.note <= (mTuning[j] + mNumFrets)) {
                        // This note can be played on this string
                        if (Math.abs((fretNote.note - mTuning[j]) - targetFret) < Math.abs(fretNote.fret - targetFret)) {
                            // This fret position is closer to the target than the current position
                            fretNote.string = j;
                            fretNote.fret = fretNote.note - mTuning[j];
                            fretNote.name = setName(fretNote.note);
                            if (fretNote.on) {
                                // Only use up a string for ON notes
                                mStringAvailable[j] = false;
                            }
                        }
                    }
                }
            }
        }
        return fretNotes;
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
