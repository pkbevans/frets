package com.bondevans.frets.instruments;

/**
 * Defines a Fretted Instrument
 */
public abstract class FretInstrument {

    public interface Instrument {
        int numStrings();
        int numFrets();
        int[] TUNING();   // Highest to lowest
        String[] STRING_NAMES();
    }
}
