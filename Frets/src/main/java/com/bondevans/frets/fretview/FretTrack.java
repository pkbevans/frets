package com.bondevans.frets.fretview;

//import com.bondevans.fretboard.utils.Log;

import com.bondevans.frets.instruments.FretInstrument;
import com.bondevans.frets.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of Fretevents making up a track
 */
public class FretTrack extends FretBase {
    private static final String TAG = FretTrack.class.getSimpleName();
    private static final String ELEMENT_TRACK = "track";
    private static final String TRACK_ELEMENT_OPEN = "<"+ELEMENT_TRACK+">";
    private static final String TRACK_ELEMENT_CLOSE = "</"+ELEMENT_TRACK+">";
    // External properties - exported/imported to/from xml
    private String name;
    public List<FretEvent> fretEvents;
    private int midiInstrument; // Midi Instrument (from GM) that will play this track
    private int fretInstrument; //  Which fret Instrument is this track designed for
    private boolean drumTrack;  // Is this a Drum track? (If it is then MidiInstrument is n/a)
    private boolean clickTrack;  // Is this a Click track? (If it is then MidiInstrument is n/a)
    private int clickTrackSize;
    private boolean merged;
    // INTERNAL PROPERTIES - NOT WRITTEN OUT IN/READ IN FROM TOSTRING()
    private int totalTicks;     // Essentially the Total time of the track
    List<Integer>clickEvents;    // Only used by FretViewer

    /**
     * Constructor
     * @param name track name
     * @param fretEvents List of fret events
     */
    public FretTrack(String name, List<FretEvent> fretEvents, int midiInstrument,
                     int fretInstrument, boolean isDrumTrack, int totalTicks){
        this.name = name;
        this.fretEvents = fretEvents;
        this.drumTrack = isDrumTrack;
        if(drumTrack){
            this.midiInstrument=0;
            this.fretInstrument=0;
        } else {
            this.midiInstrument = midiInstrument;
            this.fretInstrument = fretInstrument;
        }
        this.totalTicks = totalTicks;
        this.clickTrack = false;
        this.clickTrackSize = 0;
        this.merged = false;
    }

    /**
     * Constructor takes an XML-like string containing a complete track
     * @param track XML representation of the class
     */
    public FretTrack(String track) {
        fretEvents = new ArrayList<>();
        this.name = getTagString(track, ATTR_NAME);
        this.midiInstrument = getTagInt(track, ATTR_MIDI_INSTRUMENT);
        this.fretInstrument = getTagInt(track, ATTR_FRET_INSTRUMENT);
        this.drumTrack = getTagInt(track, ATTR_DRUM_TRACK)==1;
        this.clickTrack = getTagInt(track, ATTR_CLICK_TRACK)==1;
        this.clickTrackSize = getTagInt(track, ATTR_CLICK_TRACKSIZE);
        this.merged =  getTagInt(track, ATTR_MERGED)==1;
        loadFretEvents(track);
    }

    private static Pattern evPattern = Pattern.compile("<ev[^>]*>(.*?)</ev>",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    private void loadFretEvents(String track) {
        fretEvents = new ArrayList<>();
        Matcher matcher = evPattern.matcher(track);

        // look for contents of <ev></ev>
        while (matcher.find()) {
            String ev = matcher.group(1);
//            Log.d(TAG, "HELLO found <ev> tag: [" + ev + "]");
            fretEvents.add(new FretEvent(ev));
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Output contents to XML - used to serialize class
     * @return String representation of class
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(TRACK_ELEMENT_OPEN
                + attr(ATTR_NAME, name)
                + attr(ATTR_MIDI_INSTRUMENT, midiInstrument)
                + attr(ATTR_FRET_INSTRUMENT, fretInstrument)
                + attr(ATTR_DRUM_TRACK, drumTrack)
                + attr(ATTR_CLICK_TRACK, clickTrack)
                + attr(ATTR_CLICK_TRACKSIZE, clickTrackSize)
                + attr(ATTR_MERGED, merged)
        );
        for (FretEvent event : fretEvents) {
            sb.append(event.toString());
        }
        sb.append(TRACK_ELEMENT_CLOSE);
        return sb.toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMidiInstrument() {
        return midiInstrument;
    }

    public void setMidiInstrument(int instrument) {
        Log.d(TAG, "Setting Midi Instrument to: "+ instrument);
        this.midiInstrument = instrument;
    }

    public boolean isDrumTrack() {
        return drumTrack;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public void setDrumTrack(boolean isChecked) {
        drumTrack = isChecked;
    }

    public boolean isClickTrack() {
        return clickTrack;
    }

    public int getClickTrackSize(){
        return clickTrackSize;
    }

    public void removeEvents(){
        fretEvents.clear();
    }

    void setTrackInEvents(int track){
        for(FretEvent fretEvent: fretEvents){
            fretEvent.track = track;
        }
    }
    public void dump(String text) {
        Log.d(TAG, "DUMP "+text);
        Log.d(TAG, "TRACK,EV,TICKS,BEND");
        FretEvent fe;
        for(int i=0; i<fretEvents.size();i++){
            fe = fretEvents.get(i);
            Log.d(TAG, fe.track + ","+i + "," +fe.getTicks());
//            for(FretNote fn: fe.fretNotes) {
//                Log.d(TAG, i + "," + fe.track + "," + fn.note+","+(fn.on?"ON":"OFF")+","+fe.getTicks());
//            }
//            if(fe.bend>0){
//                Log.d(TAG, i + "," + fe.track + "," + "-"+","+"-"+","+fe.getTicks()+","+fe.bend);
//            }
        }
    }
    public int getTotalTicks() {
        return totalTicks;
    }

    /**
     * creates a click track - subsequently used by the FretViewerFragment to display the progress
     * through the song.
     *
     * @param longest - total ticks for longest track in this song
     * @param ticksPerQtrNote - ticks per quarter note (i.e. a beat)
     */
    public void createClickTrack(int longest, int ticksPerQtrNote){
        fretEvents = new ArrayList<>();
        android.util.Log.d(TAG, "createClickTrackFretEvents:"+longest);
        int totalTicks=0;
        // Add click at start
        int clickEvent=1;
        fretEvents.add(new FretEvent(clickEvent++, 0, totalTicks));
        while(totalTicks < longest){
            fretEvents.add(new FretEvent(clickEvent++, ticksPerQtrNote, totalTicks));
            totalTicks+=ticksPerQtrNote;
        }
        this.clickTrack=true;
        this.clickTrackSize=fretEvents.size();
    }
    public void generateClickEventList(){
        clickEvents = new ArrayList<>();
        int i=0;
        int remember=0;
        for(FretEvent fretEvent: fretEvents){
            if(fretEvent.isClickEvent()){
                Log.d(TAG, "HELLO generateClickEventList adding: ["+remember+"]["+fretEvent.getClickEvent()+"]");
                clickEvents.add(remember);
            }else{
                remember = i;
            }
            ++i;
        }
    }
    public int getClickEventByClickNumber(int clickNumber){
        return clickEvents.get(clickNumber);
    }
    public void setInitialFretPositions(FretInstrument.Instrument instrument){
        FretPosition fretPosition = new FretPosition(instrument);
        // read through all events in the track and set FretPositions for each
        for( FretEvent fretEvent: fretEvents){
            fretPosition.getFretPositions(fretEvent.fretNotes);
        }
    }
    public int getEventSizeForTrack(int track){
        int i=0;
        for(FretEvent fretEvent: fretEvents){
            if(fretEvent.track == track)++i;
        }
        return i;
    }
}

