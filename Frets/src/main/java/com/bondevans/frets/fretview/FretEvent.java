package com.bondevans.frets.fretview;

import com.bondevans.frets.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A FretEvent is an event that impacts on the Fretboard
 */
public class FretEvent extends FretBase {
    private static final String TAG = FretEvent.class.getSimpleName();
    static final int MAX_BEND = 10;
    // THESE ELEMENTS ARE WRITTEN OUT IN/READ IN FROM TOSTRING()
    public int deltaTicks;
    public int tempo;
    public int bend;
    public List<FretNote> fretNotes;
    public int track;
    public int totalTicks;
    private int clickEvent;
    private boolean hasOnNotes;
    // INTERNAL PROPERTIES - NOT WRITTEN OUT IN/READ IN FROM TOSTRING()

    /**
     * Constructor
     * @param deltaTicks time in ticks since previous event
     * @param fretNotes array of notes to play at the same time
     * @param tempo New tempo if > 0
     * @param bend Apply bend if > 0
     * @param totalTicks total ticks for this track as of this event
     */
    public FretEvent(int deltaTicks, List<FretNote> fretNotes, int tempo, int bend, int totalTicks) {
        this.deltaTicks = deltaTicks;
        this.tempo = tempo;
        this.bend = bend;
        this.fretNotes = fretNotes;
        this.totalTicks = totalTicks;

        this.hasOnNotes = false;
        for(FretNote fretNote: fretNotes){
            if(fretNote.on){
                this.hasOnNotes = true;
                break;
            }
        }
    }

    /**
     * Constructor - creates a click event - no notes
     * @param clickEvent - Click event number
     * @param deltaTicks - number of ticks delay
     * @param totalTicks - total ticks
     */
    public FretEvent(int clickEvent, int deltaTicks, int totalTicks){
        this.clickEvent = clickEvent;
        this.deltaTicks = deltaTicks;
        this.totalTicks = totalTicks;
        this.fretNotes =  new ArrayList<>();    // empty list of notes
        this.bend = 0;
        this.tempo = 0;
        this.hasOnNotes = false;
    }

    /**
     * Output contents of FretEvent to XML - used to serialize t disk
     * @return XML-like string of class contents
     */
    @Override
    public String toString(){
        return toJsonString();
    }

    /**
     * Returns true if this event has ON notes (or false if it only has OFF notes)
     *
     * @return True if there are ON notes in this event, else False
     */
    public boolean hasOnNotes() {
        return hasOnNotes;
    }

    public int getTicks() {
        return deltaTicks;
    }

    public void setTicks(int ticks) {
//        Log.d(TAG, "track:"+track+" setTicks:"+ticks);
        this.deltaTicks = ticks;
    }
    public String dbg(){
        return "TRACK:"+track+" TICKS:"+ deltaTicks;
    }
    /*
     * Returns a List of notes that are turned ON on this event
     */
    public List<Integer> getOnNotes(){
        List<Integer> notes = new ArrayList<>();
        for (FretNote fretNote : fretNotes) {
            if (fretNote.on) {
                notes.add(fretNote.note);
            }
        }
        return notes;
    }

    public boolean isClickEvent() {
        return clickEvent>0;
    }

    public int getClickEvent() {
        return clickEvent;
    }
    public String toLogString(){
        return
        " ticks:" +deltaTicks+
        " tempo:" +tempo+
        " bend:" +bend+
        " notes:" +fretNotes.size()+
        " track:"  +track+
        " totalTicks:" +totalTicks+
        " clickEvent:" +clickEvent+
        " " + (hasOnNotes?" HAS-ONNOTES ": " NO-ONNOTES");
    }
    public FretEvent(String json) {
        Log.d(TAG, "JSON Constructor");
        try {
            JSONObject jsonObject = new JSONObject(json);
            deltaTicks = jsonObject.getInt("deltaTicks");
            tempo = jsonObject.getInt("tempo");
            bend = jsonObject.getInt("bend");
            track = jsonObject.getInt("track");
            totalTicks = jsonObject.getInt("totalTicks");
            clickEvent = jsonObject.getInt("clickEvent");
            hasOnNotes = jsonObject.getBoolean("hasOnNotes");
            // List of FretEvents
            fretNotes = new ArrayList<>();
            if(jsonObject.has(JSON_NOTES)) {
                JSONArray fretNoteArray = jsonObject.getJSONArray(JSON_NOTES);
                for (int i = 0; i < fretNoteArray.length(); i++) {
                    fretNotes.add(new FretNote(fretNoteArray.get(i).toString()));
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
            jsonObject.put("deltaTicks", deltaTicks);
            jsonObject.put("tempo", tempo);
            jsonObject.put("bend", bend);
            jsonObject.put("track", track);
            jsonObject.put("totalTicks", totalTicks);
            jsonObject.put("clickEvent", clickEvent);
            jsonObject.put("hasOnNotes", hasOnNotes);
            // Array of FretEvents
            JSONArray fretEvents = new JSONArray();
            for(FretNote fretNote: this.fretNotes){
                fretEvents.put(fretNote.toJson());
            }
            jsonObject.putOpt(JSON_NOTES, fretEvents);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
    public String toJsonString(){
        return toJson().toString();
    }
}

