package com.bondevans.fretboard.player;

/**
 * Track name and number
 */
public class SongTrack {
    public String name;
    public int index;

    public SongTrack(String name, int index) {
        this.name = name;
        this.index = index;
    }
    public String toString(){
        return name;
    }
}
