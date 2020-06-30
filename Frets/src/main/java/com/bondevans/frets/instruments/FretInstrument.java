package com.bondevans.frets.instruments;

/**
 * Defines a Fretted Instrument
 */
public abstract class FretInstrument {
    public final static int INTRUMENT_GUITAR = 0;
    public final static int INTRUMENT_BASS = 1;
    public final static int INTRUMENT_UKELELE = 2;
    public final static String [] instrumentNames={"GUITAR","BASS", "UKELELE"};

    public interface Instrument {
        int numStrings();

        int numFrets();

        int[] TUNING();   // Highest to lowest

        String[] STRING_NAMES();
    }
}
