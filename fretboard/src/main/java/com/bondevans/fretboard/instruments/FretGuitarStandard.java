package com.bondevans.fretboard.instruments;

/**
 * Defines a Fretted Instrument
 */
public class FretGuitarStandard extends FretInstrument implements FretInstrument.Instrument {
    static final int MAX_STRINGS_GUITAR = 6;
    static final int[] GUITAR_STANDARD_TUNING = new int[]{64, 59, 55, 50, 45, 40};   // Highest to lowest
    static final String[] GUITAR_STANDARD_TUNING_STRING_NAMES = new String[]{"Top E", "B", "G", "D", "A", "Low E"};
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
