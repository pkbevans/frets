package com.bondevans.frets.fretview;

import com.bondevans.frets.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FretPlayerEvent extends FretBase {
    private static final String TAG = FretPlayerEvent.class.getSimpleName();
    public long ticks;
    public int delay;
    public int click;
    int track;
    public int bend;
    public byte [] midiBuffer;
    public List<FretNote> uiFretNotes;

    public FretPlayerEvent(long ticks, int delay, byte[] midiBuffer, int click, int track, int bend, List <FretNote> fretNotes) {
        this.ticks = ticks;
        this.delay = delay;
        this.midiBuffer = midiBuffer;
        this.click = click;
        this.track = track;
        this.bend = bend;
        this.uiFretNotes = fretNotes;
        Log.d(TAG, "HELLO Added: "+toString());
    }
    public String toString(){
        return toJson();
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
            // upper case
            // result.append(String.format("%02X", aByte));
        }
        return result.toString();
    }
    public static int calcDelay(long deltaTicks, int tempo, int ticksPerQtrNote) {
        if (deltaTicks > 0) {
            // Avoid divide by zero error
            double x = ((60 * 1000) / tempo);  // Tempo is BPM. x = number of mill-secs per beat
            double y = x / ticksPerQtrNote;    // y = how many milliseconds per tick
            double z = deltaTicks * y;          // z = how many milliseconds for given number of ticks
            return (int) z;
        }else{
            return 0;
        }
    }
    public FretPlayerEvent(String json){
        Log.d(TAG, "JSON Constructor");
        try {
            JSONObject jsonObject = new JSONObject(json);
            ticks = jsonObject.getLong("ticks");
            delay = jsonObject.getInt("delay");
            click = jsonObject.getInt("click");
            track = jsonObject.getInt("track");
            bend = jsonObject.getInt("bend");
            String midBuf = jsonObject.getString("midiBuffer");
            midiBuffer = new byte[(midBuf.length()/2)];
            for (int i = 0; i < midiBuffer.length; i++) {
                int index = i * 2;
                int j = Integer.parseInt(midBuf.substring(index, index + 2), 16);
                midiBuffer[i] = (byte) j;
            }
            JSONArray jsonArray = jsonObject.getJSONArray(JSON_UI_NOTES);
            uiFretNotes = new ArrayList<>();
            if(jsonObject.has(JSON_UI_NOTES)) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    uiFretNotes.add(new FretNote(jsonArray.get(i).toString()));
                }
            }
            Log.d(TAG, "HELLO Added from Json: "+toJson());
        } catch (JSONException e) {
            Log.e(TAG, "HELLO JSON error: "+e.getMessage());
        }
    }
    public String toJson(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ticks", ticks);
            jsonObject.put("delay", delay);
            jsonObject.put("click", click);
            jsonObject.put("track", track);
            jsonObject.put("bend", bend);
            jsonObject.put("midiBuffer", hex(midiBuffer));
            JSONArray fretNoteArray = new JSONArray();
            for (FretNote fretNote: uiFretNotes) {
                fretNoteArray.put(fretNote.toJson());
            }
            jsonObject.put(JSON_UI_NOTES, fretNoteArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}