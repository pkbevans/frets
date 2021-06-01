package com.bondevans.frets.fretview;

import com.bondevans.frets.midi.Midi;
import com.bondevans.frets.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of <class>FretTrack</class>s making up a track
 */
public class FretSong extends FretBase {
    private static final String TAG = FretSong.class.getSimpleName();
    private String name;
    private int tpqn;
    private int bpm;
    private int soloTrack;
    private List<FretTrack> fretTracks;
    private String keywords;

    /**
     * Constructor
     *
     * @param name       Song name
     * @param keywords   keywords for this fret
     * @param tpqn       Ticks per quarter note
     * @param bpm        Beats Per Minute
     * @param fretTracks List of FretTracks
     */
    public FretSong(String name, String keywords, int tpqn, int bpm, List<FretTrack> fretTracks) {
        this.name = name;
        this.tpqn = tpqn;
        this.bpm = bpm;
        if(fretTracks != null) {
            this.fretTracks = fretTracks;
        }
        this.soloTrack = 0;   // Assume first track is the solo
        this.keywords=keywords;
    }

    /**
     * Output contents to XML - used to serialize class
     *
     * @return String representation of class
     */
    @Override
    public String toString() {
        return toJsonString();
    }

    /**
     * Add a new track to the song. Initialises list if not already done
     *
     * @param fretTrack tracvk to add
     */
    public void addTrack(FretTrack fretTrack) {
        if (fretTracks == null) {
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
     *
     * @param index index of track
     * @return FretTrack at given position
     */
    public FretTrack getTrack(int index) {
        return fretTracks.get(index);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * <code>FretSong.tracks()</code>returns the number of tracks in the song
     *
     * @return the number of tracks in this song
     */
    public int tracks() {
        return fretTracks.size();
    }

    /**
     * <code>FretSong.tracksIgnoreClick()</code>returns
     *
     * @return The number of tracks in the song - not including the click track if present
     */
    public int tracksIgnoreClick() {
        int x = 0;
        for (FretTrack fretTrack : fretTracks) {
            if (!fretTrack.isClickTrack()) {
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

    public List<FretTrack> getFretTracks() {
        return fretTracks;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public void updateTrack(int track, FretTrack fretTrack) {
        fretTracks.set(track, fretTrack);
    }

    /**
     * sets the FretEvent.track attribute for all events in all tracks
     */
    public void setTrackInEvents() {
        int track = 0;
        for (FretTrack fretTrack : fretTracks) {
            fretTrack.setTrackInEvents(track++);
        }
    }

    public int getClickTrack() {
        int x = 0;
        for (FretTrack fretTrack : fretTracks) {
            if (fretTrack.isClickTrack()) {
                return x;
            }
            x++;
        }
        return 0;   // Default to 1st track if no click track
    }

    public List<FretPlayerEvent> mFretPlayerEvents;
    private FretEvent prevEvent = new FretEvent(0, 0, 0);
    private static final int MIDI_CHANNEL_DRUMS = 9;
    private static final int MIN_BEND_TICKS = 30;
    List<FretNote> uiNotes;

    public void buildFretPlayerTrack() {
        byte[] buffer;
        int channel;
        int bend;
        int delay;
        int ticks = 0;
        if (null == mFretPlayerEvents) {
            mFretPlayerEvents = new ArrayList<>();
        }
        for (FretEvent fretEvent : fretTracks.get(soloTrack).fretEvents) {
            android.util.Log.d(TAG, "HELLO Building: " + fretEvent.toLogString());
            delay = FretPlayerEvent.calcDelay(fretEvent.deltaTicks, bpm, tpqn);
            if (fretEvent.fretNotes.size() > 0 || fretEvent.bend > 0) {
                // Build the midi buffer
                buffer = new byte[(fretEvent.fretNotes.size() + (fretEvent.bend > 0 ? 1 : 0)) * 3];
                int i = 0;
                channel = fretTracks.get(fretEvent.track).isDrumTrack() ? MIDI_CHANNEL_DRUMS : fretEvent.track;
                for (FretNote fretNote : fretEvent.fretNotes) {
                    // Build the MIDI buffer
                    buffer[i++] = (byte) (fretNote.on ? (Midi.NOTE_ON | channel) : (Midi.NOTE_OFF | channel));
                    // Note value
                    buffer[i++] = (byte) fretNote.note;
                    // Velocity - Hardcoded volume for NOTE_ON and zero for NOTE_OFF
                    buffer[i++] = (byte) (fretNote.on ? 0x60 : 0x00);
                }
                if (fretEvent.bend > 0) {
                    // Send pitch Bend message (will alter current note playing)
                    buffer[i++] = (byte) (Midi.PITCH_WHEEL | channel);
                    buffer[i++] = (byte) (fretEvent.bend & 0x7F);
                    buffer[i] = (byte) ((byte) (fretEvent.bend >> 7) & 0x7F);
                }
            } else {
                buffer = new byte[0];
            }
            // Filter out some bend events for the UI and any notes NOT on the solo track
            if ((fretEvent.bend > 0 && prevEvent.bend > 0 && ticks < MIN_BEND_TICKS) || soloTrack != fretEvent.track) {
                // Ignore if this is a bender and prev event was a bender, and ticks < MIN_BEND_TICKS
                bend = 0;
                uiNotes = new ArrayList<>();
                android.util.Log.d(TAG, "HELLO Ig1: " + fretEvent.toLogString());
                ticks += fretEvent.deltaTicks;
            } else {
                bend = fretEvent.bend;
                uiNotes = fretEvent.fretNotes;
                prevEvent = fretEvent;
                ticks = 0;
            }
            mFretPlayerEvents.add(new FretPlayerEvent(fretEvent.getTicks(), delay, buffer, fretEvent.getClickEvent(), fretEvent.track, bend, uiNotes));
        }
        // REMOVE
        int x = 1;
        for (FretPlayerEvent fretPlayerEvent : mFretPlayerEvents) {
            android.util.Log.d(TAG, "HELLO Built: " + (x++) + " " + fretPlayerEvent.toString());
        }
    }

    public FretSong(String json) {
        Log.d(TAG, "JSON Constructor");
        try {
            JSONObject jsonObject = new JSONObject(json);
            name = jsonObject.getString("name");
            tpqn = jsonObject.getInt("tpqn");
            bpm = jsonObject.getInt("bpm");
            soloTrack = jsonObject.getInt("soloTrack");
            keywords = jsonObject.getString("keywords");
            // List of Tracks
            fretTracks = new ArrayList<>();
            if (jsonObject.has(JSON_TRACKS)) {
                JSONArray fretTrackArray = jsonObject.getJSONArray(JSON_TRACKS);
                for (int i = 0; i < fretTrackArray.length(); i++) {
                    fretTracks.add(new FretTrack(fretTrackArray.get(i).toString()));
                }
            }
            // List of FretPlayerEvents
            mFretPlayerEvents = new ArrayList<>();
            if (jsonObject.has(JSON_PLAYER_EVENTS)) {
                Log.d(TAG, "HELLO Got player events");
                JSONArray fretPlayerEventArray = jsonObject.getJSONArray(JSON_PLAYER_EVENTS);
                for (int i = 0; i < fretPlayerEventArray.length(); i++) {
                    mFretPlayerEvents.add(new FretPlayerEvent(fretPlayerEventArray.get(i).toString()));
                }
            }
            Log.d(TAG, "HELLO Added from Json: " + toJsonString());
        } catch (JSONException e) {
//            fretSongOld(json);  // Old style xml
            Log.e(TAG, "HELLO JSON error: " + e.getMessage());
        }
    }

    public JSONObject toJson() {
        Log.d(TAG, "HELLO to json");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            jsonObject.put("tpqn", tpqn);
            jsonObject.put("bpm", bpm);
            jsonObject.put("soloTrack", soloTrack);
            jsonObject.put("keywords", keywords);
            // Array of FretTracks
            JSONArray fretTracksJson = new JSONArray();
            Log.d(TAG, "HELLO got tracks: " + fretTracks.size());
            for (FretTrack fretTrack : fretTracks) {
                fretTracksJson.put(fretTrack.toJson());
            }
            jsonObject.putOpt(JSON_TRACKS, fretTracksJson);
            if (mFretPlayerEvents != null && mFretPlayerEvents.size() > 0) {
                Log.d(TAG, "HELLO got fret player events: " + mFretPlayerEvents.size());
                // Array of FretPlayerEvents
                JSONArray fretPlayerEvents = new JSONArray();
                for (FretPlayerEvent fretPlayerEvent : mFretPlayerEvents) {
                    fretPlayerEvents.put(fretPlayerEvent.toJson());
                }
                jsonObject.putOpt(JSON_PLAYER_EVENTS, fretPlayerEvents);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String toJsonString() {
        return toJson().toString();
    }
}
