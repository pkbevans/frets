package com.bondevans.frets.instruments;

/**
 * Defines a Fretted Instrument
 */
public class FretBassGuitarStandard extends FretInstrument implements FretInstrument.Instrument {
    static final int MAX_STRINGS_BASS = 4;
    static final int[] BASS_STANDARD_TUNING = new int[]{43, 38, 33, 28};   // Highest to lowest
    static final String[] BASS_STANDARD_TUNING_STRING_NAMES = new String[]{"G", "D", "A", "E"};
    static final int MAX_FRETS_BASS = 20;

    @Override
    public int numStrings() {
        return MAX_STRINGS_BASS;
    }

    @Override
    public int numFrets() {
        return MAX_FRETS_BASS;
    }

    @Override
    public int[] TUNING() {
        return BASS_STANDARD_TUNING;
    }

    @Override
    public String[] STRING_NAMES() {
        return BASS_STANDARD_TUNING_STRING_NAMES;
    }
}
