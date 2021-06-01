package com.bondevans.frets.fretview;

import com.bondevans.frets.utils.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A note on the fretboard
 */
public class FretNote extends FretBase  implements Comparable<FretNote>{
    private static final String TAG = FretNote.class.getSimpleName();
    private static final int NOT_SET = 99;
    public int note;        // Midi note value (Bottom E on standard Guitar is 40)
    public boolean on;      // True = turn note ON, False = turn FretNote off
    int string;             // String number from 0 to mStrings (0=highest string - i.e. Top E on standard guitar)
    int fret;               // fret position
    int bend;               // bend value applied to this note
    String name;            // FretNote name (e.g. E, F#, etc)

    /**
     * Constructor takes note, ON/OFF and fret details
     *
     * @param note   Note midi value
     * @param on     Note on (true) or off (false)
     * @param string String to play note on
     * @param fret   Fret to play note on
     * @param name   Name of the note
     */
    public FretNote(int note, boolean on, int string, int fret, String name) {
        this.note = note;
        this.on = on;
        this.string = string;
        this.fret = fret;
        this.name = name;
        this.bend = 0;
    }
    /**
     * Constructor sets note and ON/OFF
     *
     * @param note Midi note value
     * @param on true = on false = off
     */
    public FretNote(int note, boolean on) {
        this.note = note;
        this.on = on;
        this.string = NOT_SET;
        this.fret = NOT_SET;
        this.name = "";
        this.bend = 0;
    }
    public FretNote(FretNote fretNote) {
        this.note = fretNote.note;
        this.on = fretNote.on;
        this.string = fretNote.string;
        this.fret = fretNote.fret;
        this.name = fretNote.name;
        this.bend = fretNote.bend;
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    @Override
    public int compareTo(FretNote f) {
        return this.note-f.note;
    }
    public FretNote(String jsonString){
        Log.d(TAG, "JSON Constructor");
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            note = jsonObject.getInt("note");
            on = jsonObject.getBoolean("on");
            string = jsonObject.getInt("string");
            fret = jsonObject.getInt("fret");
            name = jsonObject.getString("name");
        } catch (JSONException e) {
            Log.e(TAG, "HELLO JSON error: "+e.getMessage());
        }
    }
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("note", note);
            jsonObject.put("on", on);
            jsonObject.put("string", string);
            jsonObject.put("fret", fret);
            jsonObject.put("bend", bend);
            jsonObject.put("name", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
    public String toJsonString(){
        return toJson().toString();
    }
}
