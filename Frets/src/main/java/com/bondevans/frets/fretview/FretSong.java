package com.bondevans.frets.fretview;

import com.bondevans.frets.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of <class>FretTrack</class>s making up a track
 */
public class FretSong extends FretBase {
    private static final String TAG = FretSong.class.getSimpleName();
    private static final String ELEMENT_SONG = "song";
    private static final String SONG_ELEMENT_OPEN = "<"+ELEMENT_SONG+">";
    private static final String SONG_ELEMENT_CLOSE = "</"+ELEMENT_SONG+">";
    private String name;
    private int tpqn;
    private int bpm;
    private int soloTrack;
    private List<FretTrack> fretTracks;
    private String keywords;

    /**
     * Constructor
     * @param name Song name
     * @param keywords keywords for this fret
     * @param tpqn Ticks per quarter note
     * @param bpm Beats Per Minute
     * @param fretTracks List of FretTracks
     */
    public FretSong(String name, String keywords, int tpqn, int bpm, List<FretTrack> fretTracks){
        this.name = name;
        this.tpqn = tpqn;
        this.bpm = bpm;
        if(fretTracks != null) {
            this.fretTracks = fretTracks;
        }
        this.soloTrack = 0;   // Assume first track is the solo
        this.keywords=keywords;
    }

    public FretSong(String song) {
//        Log.d(TAG, "song=[" + song + "]");
        fretTracks = new ArrayList<>();
        this.name = getTagString(song, ATTR_NAME);
        this.tpqn = getTagInt(song, ATTR_TPQN);
        this.bpm = getTagInt(song, ATTR_BPM);
        this.soloTrack = getTagInt(song, ATTR_SOLO);
        this.keywords=getTagString(song, ATTR_KEYW);
        loadFretTracks(song);
    }

    private static Pattern trackPattern = Pattern.compile("<track[^>]*>(.*?)</track>",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    private void loadFretTracks(String song) {
        fretTracks= new ArrayList<>();
        Matcher matcher = trackPattern.matcher(song);

        // look for contents of <ev></ev>
        while (matcher.find()) {
            String track = matcher.group(1);
//            Log.d(TAG, "HELLO found "+FretTrack.ELEMENT_TRACK+" tag: [" + track + "]");
            fretTracks.add(new FretTrack(track));
        }
    }

    /**
     * Output contents to XML - used to serialize class
     * @return String representation of class
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(SONG_ELEMENT_OPEN
                +attr(ATTR_NAME, name)
                +attr(ATTR_TPQN, tpqn)
                +attr(ATTR_BPM, bpm)
                +attr(ATTR_SOLO, soloTrack)
                +attr(ATTR_KEYW, keywords)
        );
        for(FretTrack track: fretTracks){
            sb.append(track.toString());
        }
        sb.append(SONG_ELEMENT_CLOSE);
        Log.d(TAG, "XML: " + sb.toString());
        return sb.toString();
    }

    /**
     * Add a new track to the song. Initialises list if not already done
     * @param fretTrack tracvk to add
     */
    public void addTrack(FretTrack fretTrack) {
        if(fretTracks == null){
            fretTracks = new ArrayList<>();
        }
        fretTracks.add(fretTrack);
    }

    public String getName() {
        return name;
    }

    public int getTpqn() {
        return tpqn;
    }

    public int getBpm() {
        return bpm;
    }

    /**
     * Get specified track
     * @param index index of track
     * @return FretTrack at given position
     */
    public FretTrack getTrack(int index){
        return fretTracks.get(index);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * <code>FretSong.tracks()</code>returns the number of tracks in the song
     * @return the number of tracks in this song
     */
    public int tracks() {
        return fretTracks.size();
    }

    /**
     * <code>FretSong.tracksIgnoreClick()</code>returns the number of tracks in the song - not
     * including the click track if present
     * @return
     */
    public int tracksIgnoreClick() {
        int x=0;
        for(FretTrack fretTrack: fretTracks){
            if( !fretTrack.isClickTrack()){
                x++;
            }
        }
        return x;
    }

    public void deleteTrack(int track) {
        fretTracks.remove(track);
        // Adjust solo track if necessary
        if (soloTrack == track) {
            soloTrack = 0;
        } else if (soloTrack > track) {
            soloTrack--;
        }
    }

    public void setSoloTrack(int track) {
        this.soloTrack = track;
    }

    public int getSoloTrack() {
        return this.soloTrack;
    }

    public String getTrackName(int track) {
        return fretTracks.get(track).getName();
    }
    public List<FretTrack> getFretTracks(){
        return fretTracks;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    public void updateTrack(int track, FretTrack fretTrack){
        fretTracks.set(track,fretTrack);
    }

    /**
     * sets the FretEvent.track attribute for all events in all tracks
     */
    public void setTrackInEvents(){
        int track=0;
        for(FretTrack fretTrack: fretTracks){
            fretTrack.setTrackInEvents(track++);
        }
    }

    public int getClickTrack() {
        int x=0;
        for(FretTrack fretTrack: fretTracks){
            if( fretTrack.isClickTrack()){
                return x;
            }
            x++;
        }
        return 0;   // Default to 1st track if no click track
    }
}

