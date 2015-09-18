package com.bondevans.fretboard.fretboardplayer;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and parses midi file mData
 */
public class MidiFile {
    private static final String TAG = "MidiFile";
    private static final int FILE_HEADER_LENGTH = 14;
    private static final int BUF_LEN = 8192;
    private static final int TRACK_HEADER_LENGTH = 8;

    private String mMidiFilePath;
    private int mFormat; //  0=single track, 1=multiple mTracks
    private int mTracks; //  Number of mTracks in this file
    private int mTicksPerBeat;
//    private String[] mTrackNameOld;
    private List<String> mTrackNames;
    private int[] mTrackChunkLength;
    private boolean mHeaderLoaded = false;
    private List<NoteEvent> mNoteEvents = new ArrayList<>();
    private int mRunningStatus = 0;
    private int mRunningChannel = 0;


    MidiFile(String midiFile) throws FretBoardException {
        // Open up file and load header and track details
        this.mMidiFilePath = midiFile;

        loadHeader(mMidiFilePath);
        mHeaderLoaded = true;
    }

    public List<String> getTrackNames() throws FretBoardException {
        if (!mHeaderLoaded) {
            throw new FretBoardException("ERRROR - Header not loaded");
        }

//        return mTrackNameOld;
        return mTrackNames;
    }

    /**
     * Loads the specified track into a byte array
     *
     * @param track Track number (1st = 0)
     * @return Returns byte buffer containing track mData
     */
    private byte[] loadTrack(int track) throws FretBoardException {
        Log.d(TAG, "loadTrack [" + track + "]");
        BufferedInputStream in;
        byte[] buffer = new byte[mTrackChunkLength[track]];
        // Work out the offset to the specified track mData
        long skip = FILE_HEADER_LENGTH;
        int t = 0;
        while (t < track) {
            skip += TRACK_HEADER_LENGTH + mTrackChunkLength[t];
            ++t;
        }
        skip += TRACK_HEADER_LENGTH;
        // Read the track mData
        try {
            in = new BufferedInputStream(new FileInputStream(mMidiFilePath), BUF_LEN);
        } catch (FileNotFoundException e) {
            throw new FretBoardException("ERROR-reading track");
        }
        try {
            if (in.skip(skip) != skip) {
                throw new FretBoardException("ERROR-skipping");
            }
            if (in.read(buffer, 0, mTrackChunkLength[t]) != mTrackChunkLength[t]) {
                throw new FretBoardException("ERROR-reading track");
            }
            in.close();
        } catch (IOException e) {
            throw new FretBoardException(e.getMessage());
        }
        return buffer;
    }

    public List<NoteEvent> loadNoteEvents(int track) throws FretBoardException, IOException {
        Log.d(TAG, "loadNoteEvents");
        InputStream in = new ByteArrayInputStream(loadTrack(track));
        MidiEvent ev = getEvent(in);
        int runningTicks = 0;

        while (!ev.isEndOfTrack()) {
            Log.d(TAG, "Got event");
            if (ev.isNoteOnOrOff()) {
                Log.d(TAG, "Got Note event");
                // If velocity is zero then this effectively a FretNote Off message, so we'll store it as such
                if(ev.mParam2 == 0 && MidiEvent.NOTE_EVENT_TYPE_NOTE_ON == ev.mNoteEventType){
                    ev.mNoteEventType = MidiEvent.NOTE_EVENT_TYPE_NOTE_OFF;
                }
                mNoteEvents.add(new NoteEvent(ev.mParam1, ev.mNoteEventType == MidiEvent.NOTE_EVENT_TYPE_NOTE_ON, ev.mTicks + runningTicks));
                runningTicks = 0;
            } else {
                // need to keep running total of mTicks for events that we ignore.
                runningTicks += ev.mTicks;
            }
            ev = getEvent(in);
        }
        return mNoteEvents;
    }

    /**
     * Gets the next event in the midi track
     * @param in Track InputStream
     * @return MidiEvent
     * @throws IOException
     */
    private MidiEvent getEvent(InputStream in) throws IOException {
        int param1, param2 = 0;
        int channel;
        int ticks = getTimeTicks(in);
        int type = in.read();
        Log.d(TAG, "TYPE["+iToHex(type)+"]");
        // What sort of event is it?
        if (type == 0xff) {
            int len;
            // Meta event (or END OF TRACK)
            int metaType = in.read();
            len = getVariableLen(in);
            if (metaType == MidiEvent.META_EVENT_TYPE_END_OF_TRACK) {
                Log.d(TAG, "END OF TRACK");
            } else {
                Log.d(TAG, "META EVENT=mNoteEventType[" + metaType + "] mLen["+len+"]");
            }
            return new MidiEvent(MidiEvent.TYPE_META_EVENT, metaType, len, in);
        } else if (type == 0xf0) {
            Log.d(TAG, "SYSEX - OOPS TODO TODO TODO");
            int len = getVariableLen(in);
            return new MidiEvent(MidiEvent.TYPE_SYSEX_EVENT, 0, len, in);
        } else {
            // Must be a FretNote event - mNoteEventType is top 4 bits
            int noteEventType = (type & 0xf0) >> 4;
            // Check whether its a running status
            if (noteEventType < MidiEvent.NOTE_EVENT_TYPE_NOTE_OFF) {
                // Must be running status, so this on is actually mParam1
                param1 = type;
                noteEventType = mRunningStatus;
                channel = mRunningChannel;
                Log.d(TAG, "Running status [" + noteEventType + "]");
            } else {
                // mChannel = bottom 4 bits
                channel = type & 0x0f;
                param1 = in.read();
            }

            switch (noteEventType) {
                case MidiEvent.NOTE_EVENT_TYPE_NOTE_OFF:
                case MidiEvent.NOTE_EVENT_TYPE_NOTE_ON:
                    // 1st byte is note, 2nd byte is velocity
                    param2 = in.read();
                    Log.d(TAG, "NOTE_OFF/ON [" + noteEventType + "]["+iToHex(param1)+"]["+iToHex(param2)+"]["+noteName(param1)+"]");
                    break;
                case MidiEvent.NOTE_EVENT_TYPE_NOTE_AFTER_TOUCH:
                case MidiEvent.NOTE_EVENT_TYPE_CONTROLLER:
                case MidiEvent.NOTE_EVENT_TYPE_PITCHBEND:
                    param2 = in.read();
                    Log.d(TAG, "NOTE_AFTER_TOUCH/CONTROLLER/PICTHBEND [" + noteEventType + "]["+iToHex(param1)+"]["+iToHex(param2)+"]");
                    break;
                case MidiEvent.NOTE_EVENT_TYPE_PROGRAM_CHANGE:
                case MidiEvent.NOTE_EVENT_TYPE_CHANNEL_AFTERTOUCH:
                    Log.d(TAG, "PROGRAM_CHANGE/AFTERTOUCH [" + noteEventType + "]["+iToHex(param1)+"]");
                    break;
                default:
                    Log.d(TAG, "OOPS - SOMETHING WENT WRONG [" + noteEventType + "]");
                    break;
            }
            mRunningStatus = noteEventType;
            mRunningChannel = channel;
            return new MidiEvent(ticks, noteEventType, channel, param1, param2);
        }
    }

    /**
     * Get musical note name from midi numnber
     * @param note Midi Note
     * @return Note name
     */
    private String noteName(int note) {
        int div = note/12;
        int mod = note%12;
        String [] name = {"C", "C#","D","Eb", "E", "F", "F#", "G", "Ab", "A","B", "Bb"};
        return name[mod]+"("+(div-1)+")";
    }

    private int getVariableLen(InputStream in) throws IOException {
        // Actually same as mTicks...
        return getTimeTicks(in);
    }

    /**
     * Get the number of mTicks betfore the midi event - variable length 7bit/byte
     * @param in track InputStream
     * @return mTicks
     * @throws IOException
     */
    private int getTimeTicks(InputStream in) throws IOException {
        int loop = 0;
        int ticks=0;
        int trackByte = in.read();
        while ((trackByte & 0x80) > 0) {
            ticks += trackByte & 0x7F << (loop++ * 7);
            Log.d(TAG, "getTimeTicks=[" + ticks + "]");
            trackByte = in.read();
        }
        ticks += trackByte & 0x7F << (loop++ * 7);
        return ticks;
    }

    /**
     * Load midi file header and set up the track names and lengths
     *
     * @param filePath Path to midi file
     * @throws FretBoardException
     */
    public void loadHeader(String filePath) throws FretBoardException {
        Log.d(TAG, "loadHeader");
        BufferedInputStream in;
        byte[] buffer = new byte[BUF_LEN];

        // Open up the file and read the header
        try {
            in = new BufferedInputStream(new FileInputStream(filePath), BUF_LEN);
            if (in.read(buffer, 0, FILE_HEADER_LENGTH) != FILE_HEADER_LENGTH) {
                //error - abort
                throw new FretBoardException("Error - Reading file header");
            }
            this.mFormat = buffer[9] & 0xFF;
            this.mTracks = buffer[11] & 0xFF;
            this.mTicksPerBeat = buffer[13] & 0xFF;
            Log.d(TAG, "format=" + mFormat + " tracks=" + mTracks + " ticksperbeat=" + mTicksPerBeat);

//            this.mTrackNameOld = new String[mTracks];
            this.mTrackNames = new ArrayList<>();
            this.mTrackChunkLength = new int[mTracks];

            // For each track
            // Now get the track details
            int t = 0;
            while (t < mTracks) {
                // read track header
                if (in.read(buffer, 0, TRACK_HEADER_LENGTH) != TRACK_HEADER_LENGTH) {
                    throw new FretBoardException("Error - Reading track header");
                }
                int trackLen = ((buffer[4] & 0xFF) << 24) + ((buffer[5] & 0xFF) << 16) + ((buffer[6] & 0xFF) << 8) + ((buffer[7] & 0xFF));
                Log.d(TAG, "Track: " + t + " mLen: " + trackLen);
                // Get Track name
                in.mark(BUF_LEN);
//                mTrackNameOld[t] = getTrackName(in);
                mTrackNames.add(getTrackName(in));
                in.reset();
                // SKIP track mData
                if (in.skip(trackLen) != trackLen) {
                    throw new FretBoardException("Error - Reading track header");
                }
                mTrackChunkLength[t] = trackLen;
                ++t;
            }
            in.close();
        } catch (IOException e) {
            throw new FretBoardException(e.getMessage());
        }
    }

    /**
     * Searches for a Trackname meta-event in the given InputStream
     *
     * @param in BufferedInputStream containing track mData
     * @return Track name or instrument name
     * @throws IOException
     */
    private String getTrackName(BufferedInputStream in) throws IOException {
        // Get the first event
        MidiEvent ev = getEvent(in);
        while (!ev.isEndOfTrack()) {
            if (ev.mEventType == MidiEvent.TYPE_META_EVENT &&
                    (ev.mMetaEventType == MidiEvent.META_EVENT_TYPE_TRACK_NAME ||
                            ev.mMetaEventType == MidiEvent.META_EVENT_TYPE_INSTRUMENT_NAME)) {
                return ev.getMetaDataString();
            }
            // Get the next event
            ev = getEvent(in);
        }
        return "UNKNOWN";
    }

    /**
     * Convert Int to a hex string
     * @param in
     * @return
     */
    public static String iToHex(int in){
        String hex = Integer.toHexString(in);
        if( hex.length()<2){
            hex = "0" + hex;
        }
        return hex;
    }

    public int getTicksPerBeat() {
        return mTicksPerBeat;
    }
}

class MidiEvent {
    public static final int TYPE_META_EVENT = 1;
    public static final int TYPE_SYSEX_EVENT = 2;
    public static final int TYPE_NOTE_EVENT = 3;
    public static final int NOTE_EVENT_TYPE_NOTE_OFF = 8;
    public static final int NOTE_EVENT_TYPE_NOTE_ON = 9;
    public static final int NOTE_EVENT_TYPE_NOTE_AFTER_TOUCH = 10;
    public static final int NOTE_EVENT_TYPE_CONTROLLER = 11;
    public static final int NOTE_EVENT_TYPE_PROGRAM_CHANGE = 12;
    public static final int NOTE_EVENT_TYPE_CHANNEL_AFTERTOUCH = 13;
    public static final int NOTE_EVENT_TYPE_PITCHBEND = 14;
    public static final int META_EVENT_TYPE_TRACK_NAME = 3;
    public static final int META_EVENT_TYPE_INSTRUMENT_NAME = 4;
    public static final int META_EVENT_TYPE_END_OF_TRACK = 0x2f;

    int mChannel;
    int mTicks;
    int mEventType;
    int mMetaEventType;
    int mNoteEventType;
    int mLen;
    byte[] mData;
    int mParam1;
    int mParam2;

    /**
     * Constructs a new META EVENT, SYSEX (EOT EVENT is a on of META EVENT
     *
     * @param eventType What sort of event is it - must be either TYPE_META_EVENT or TYPE_SYSEX_EVENT
     * @param metaEventType IF mEventType is TYPE_META_EVENT then this is the meta event on (aka status)
     * @param len Length of the Meta event mData
     * @param in InputStream
     */
    public MidiEvent(int eventType, int metaEventType, int len, InputStream in) {
        this.mEventType = eventType;
        this.mMetaEventType = metaEventType;
        this.mLen = len;
        mData = new byte[len];
        try {
            in.read(mData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMetaDataString() {
        if (mEventType == TYPE_META_EVENT) {
            return new String(mData);
        } else {
            return "";
        }
    }

    /**
     * Constructs a new NOTE Event
     *
     * @param ticks Ticks delay before firing this event
     * @param type What sort of note event is it
     * @param channel The midi mChannel (not used)
     * @param note The note
     * @param velocity Velocity (not used)
     */
    public MidiEvent(int ticks, int type, int channel, int note, int velocity) {
        this.mEventType = TYPE_NOTE_EVENT;
        this.mNoteEventType = type;
        this.mTicks = ticks;
        this.mChannel = channel;
        this.mParam1 = note;
        this.mParam2 = velocity;
    }

    public boolean isNoteOnOrOff() {
        return mEventType == TYPE_NOTE_EVENT &&
                (mNoteEventType == NOTE_EVENT_TYPE_NOTE_ON || mNoteEventType == NOTE_EVENT_TYPE_NOTE_OFF);
    }
    public boolean isEndOfTrack(){
        return mEventType == TYPE_META_EVENT &&
                mMetaEventType == META_EVENT_TYPE_END_OF_TRACK;
    }
}