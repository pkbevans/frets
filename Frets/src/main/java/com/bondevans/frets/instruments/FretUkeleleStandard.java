package com.bondevans.frets.instruments;

/**
 * Defines a Fretted Instrument
 */
public class FretUkeleleStandard extends FretInstrument implements FretInstrument.Instrument {
    static final int MAX_STRINGS_GUITAR = 4;
    static final int[] GUITAR_STANDARD_TUNING = new int[]{69, 64, 60, 67};   // Highest to lowest
    static final String[] GUITAR_STANDARD_TUNING_STRING_NAMES = new String[]{"A", "E", "C", "G"};
    static final int MAX_FRETS_GUITAR = 12;

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
