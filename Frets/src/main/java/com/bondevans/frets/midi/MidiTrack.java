package com.bondevans.frets.midi;

/**
 * Track name and number
 */
public class MidiTrack {
    public String name;
    public int index;

    public MidiTrack(String name, int index) {
        this.name = name;
        this.index = index;
    }
    public String toString(){
        return name;
    }
}
