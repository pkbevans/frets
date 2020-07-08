package com.bondevans.frets.instruments;

public enum Instrument {
    GUITAR_STANDARD_TUNING(0,"Guitar - EADGBE", new FretGuitarStandard())
    ,GUITAR_G_TUNING(1,"Guitar - DGDGBD", new FretGuitarGTuned())
    ,BASS_STANDARD_TUNING(2,"Bass - EAGB", new FretBassGuitarStandard())
    ,UKELELE_STANDARD_TUNING(3,"Ukelele - GCEA", new FretBassGuitarStandard())
    ;

    private final int id;
    private final String description;
    private final FretInstrument.Instrument instrument;

    Instrument(int id, String description, FretInstrument.Instrument instrument){
        this.id = id;
        this.description = description;
        this.instrument = instrument;
    }
    public int getId(){
        return this.id;
    }
    public String getDescription(){
        return this.description;
    }

    @Override
    public String toString() {
        return this.description;
    }

    public FretInstrument.Instrument getInstrument() {
        return instrument;
    }
}
