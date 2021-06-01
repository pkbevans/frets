package com.bondevans.frets.fretview;

import com.bondevans.frets.instruments.FretInstrument;
import com.bondevans.frets.instruments.Instrument;
import com.bondevans.frets.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
/**
 * A collection of Fretevents making up a track
 */
public class FretTrack extends FretBase {
    public static final int NO_FRET_INSTRUMENT = 999;
    private static final String TAG = FretTrack.class.getSimpleName();
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
    private List<Integer>clickEvents;    // Only used by FretViewer
    private FretPosition fretPosition;
    private boolean mFollowNotes;

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
        // Set default FretPositions - unless its a no-fret instrument
        if(this.fretInstrument != NO_FRET_INSTRUMENT) {
            FretInstrument.Instrument instrument = Instrument.values()[this.fretInstrument].getInstrument();
            setDefaultFretPositions(instrument);
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
        return toJsonString();
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
    public int getFretInstrument() {
        return fretInstrument;
    }
    public FretInstrument.Instrument getInstrument(){
        return Instrument.values()[fretInstrument].getInstrument();
    }
    public void setFretInstrument(int fretInstrument) {
        if(this.fretInstrument != fretInstrument) {
            this.fretInstrument = fretInstrument;
            // Need to set initial FretPositions if we change the instrument
            FretInstrument.Instrument instrument = Instrument.values()[fretInstrument].getInstrument();
            setDefaultFretPositions(instrument);
        }
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
    public void setFollowNotes(boolean followNotes) {
        this.mFollowNotes = followNotes;
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
//            for(fretNoteOld fn: fe.fretNotes) {
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
        Log.d(TAG, "createClickTrackFretEvents:"+longest);
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

    /**
     * Creates a list of the FretEvent index of each clickevent.  This is so that the FretViewer
     * can move to the correct FretEvent when the user moves the seekbar
     */
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
    private void setDefaultFretPositions(FretInstrument.Instrument instrument){
        FretPosition fretPosition = new FretPosition(instrument);
        // read through all events in the track and set FretPositions for each
        for( FretEvent fretEvent: fretEvents){
            fretPosition.setDefaultFretPositions(fretEvent.fretNotes);
        }
    }
    public int getEventSizeForTrack(int track){
        int i = 0;
        if(merged) {
            for (FretEvent fretEvent : fretEvents) {
                if (fretEvent.track == track && fretEvent.hasOnNotes()) ++i;
            }
        } else {
            for (FretEvent fretEvent : fretEvents) {
                if (fretEvent.hasOnNotes()) ++i;
            }
        }
        return i;
    }
    private void setFretPosition(){
        FretInstrument.Instrument instrument = Instrument.values()[this.fretInstrument].getInstrument();
        fretPosition = new FretPosition(instrument);
    }
    public boolean moveNotes(int mCurrentEvent, boolean up){
        boolean edited = false;
        if(fretPosition == null){
            setFretPosition();
        }
        // Save current Note(s)
        List<Integer> saveNotes = fretEvents.get(mCurrentEvent).getOnNotes();
        int event = mCurrentEvent;
        // Move the current note(s) and also move the notes in the next event if they are the same
        while(event<fretEvents.size() &&
                (saveNotes.equals(fretEvents.get(event).getOnNotes()) ||
                        // Ignore events that only have off notes
                        !fretEvents.get(event).hasOnNotes()) ) {
            // Get event
            FretEvent fretEvent = fretEvents.get(event);
            // Get the list of notes at event
            List<FretNote> notes = fretEvent.fretNotes;
            if(fretPosition.moveNotes(notes, up)){
                edited = true;
            }
            ++event;
        }
        // If edited move all following (subsequent) notes of the same value to the same fret position - only if there
        // is only one on note though - ignore chords
        if(mFollowNotes && edited && saveNotes.size() == 1){
            List<FretNote> fretNotes = fretEvents.get(mCurrentEvent).fretNotes;
            for(FretNote fretNote: fretNotes){
                if(fretNote.on) {
                    setPositionForNote(mCurrentEvent, fretNote.note,fretNote.string, fretNote.fret );
                }
            }
        }
        return edited;
    }

    /**
     * setPositionForNote - finds all instances of a given note and sets the fret position
     * for each one to specified string/position
     * @param note Midi note value
     * @param string String
     * @param fret Fret
     */
    private void setPositionForNote(int startEvent, int note, int string, int fret){
        int i=0;
        for(FretEvent fretEvent: fretEvents) {
            if(i>startEvent){
                // only change single on-note events - ignore chords/double stops
                if(fretEvent.getOnNotes().size()==1) {
                    for (FretNote fretNote : fretEvent.fretNotes) {
                        if (fretNote.on && fretNote.note == note) {
                            Log.d(TAG, "HELLO Updating follow on event: "+ i );
                            fretNote.string = string;
                            fretNote.fret = fret;
                        }
                    }
                }
            }
            ++i;
        }
    }

    public void groupNotesAtFret(int targetFret){
        if(fretPosition == null){
            setFretPosition();
        }
        // Go through the FretNotes
        for (FretEvent fretEvent: fretEvents) {
            fretPosition.setFretPositionsAtSpecifiedFret(fretEvent.fretNotes, targetFret);
        }
    }
    public FretTrack(String json) {
        Log.d(TAG, "JSON Constructor");
        try {
            JSONObject jsonObject = new JSONObject(json);
            name = jsonObject.getString("name");
            midiInstrument = jsonObject.getInt("midiInstrument");
            fretInstrument = jsonObject.getInt("fretInstrument");
            drumTrack = jsonObject.getBoolean("drumTrack");
            clickTrack = jsonObject.getBoolean("clickTrack");
            clickTrackSize = jsonObject.getInt("clickTrackSize");
            merged = jsonObject.getBoolean("merged");
            // List of FretEvents
            fretEvents = new ArrayList<>();
            if(jsonObject.has(JSON_EVENTS)) {
                JSONArray fretEventArray = jsonObject.getJSONArray(JSON_EVENTS);
                for (int i = 0; i < fretEventArray.length(); i++) {
                    fretEvents.add(new FretEvent(fretEventArray.get(i).toString()));
                }
            }
            Log.d(TAG, "HELLO Added from Json: " + toJson());
        } catch (JSONException e) {
            Log.e(TAG, "HELLO JSON error: " + e.getMessage());
        }
    }
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            jsonObject.put("midiInstrument", midiInstrument);
            jsonObject.put("fretInstrument", fretInstrument);
            jsonObject.put("drumTrack", drumTrack);
            jsonObject.put("clickTrack", clickTrack);
            jsonObject.put("clickTrackSize", clickTrackSize);
            jsonObject.put("merged", merged);
            // Array of FretEvents
            JSONArray fretEvents = new JSONArray();
            for(FretEvent fretEvent: this.fretEvents){
                fretEvents.put(fretEvent.toJson());
            }
            jsonObject.putOpt(JSON_EVENTS, fretEvents);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
    public String toJsonString(){
        return toJson().toString();
    }
}
