package com.bondevans.frets.fretview;

//import com.bondevans.fretboard.utils.Log;

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
    private String name;
    public List<FretEvent> fretEvents;
    private int midiInstrument; // Midi Instrument (from GM) that will play this track
    private int fretInstrument; //  Which fret Instrument is this track designed for
    private boolean drumTrack;  // Is this a Drum track? (If it is then MidiInstrument is n/a

    /**
     * Constructor
     * @param name Songs name
     * @param fretEvents List of fret events
     */
    public FretTrack(String name, List<FretEvent> fretEvents, int midiInstrument, int fretInstrument, boolean isDrumTrack){
        this.name = name;
        this.fretEvents = fretEvents;
        this.midiInstrument=midiInstrument;
        this.fretInstrument=fretInstrument;
        this.drumTrack = isDrumTrack;
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

    public void setDrumTrack(boolean isChecked) {
        drumTrack = isChecked;
    }

    public void removeEvents(){
        fretEvents.clear();
    }

    public void setTrackInEvents(int track){
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
}

