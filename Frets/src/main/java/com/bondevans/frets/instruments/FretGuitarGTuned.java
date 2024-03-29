package com.bondevans.frets.instruments;

/**
 * Defines a Fretted Instrument
 */
public class FretGuitarGTuned extends FretInstrument implements FretInstrument.Instrument {
    static final int MAX_STRINGS_GUITAR = 6;
    static final int[] GUITAR_STANDARD_TUNING = new int[]{62, 59, 55, 50, 43, 38};   // Highest to lowest
    static final String[] GUITAR_STANDARD_TUNING_STRING_NAMES = new String[]{"D", "B", "G", "D", "G", "D"};
    static final int MAX_FRETS_GUITAR = 20;

    @Override
    public int numStrings() {
        return MAX_STRINGS_GUITAR;
    }

    @Override
    public int numFrets() {
        return MAX_FRETS_GUITAR;
    }

    @Override
    public int[] TUNING() {
        return GUITAR_STANDARD_TUNING;
    }

    @Override
    public String[] STRING_NAMES() {
        return GUITAR_STANDARD_TUNING_STRING_NAMES;
    }
}
