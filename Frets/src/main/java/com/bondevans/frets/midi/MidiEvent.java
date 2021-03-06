package com.bondevans.frets.midi;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

class MidiEvent {
    private static final String TAG = MidiEvent.class.getSimpleName();
    static final int MICROSECONDS_PER_MINUTE = 60000000;
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
    public static final int META_EVENT_TYPE_TRACK_SEQ = 0;
    public static final int META_EVENT_TYPE_TRACK_TEXT = 1;
    public static final int META_EVENT_TYPE_TRACK_COPYRIGHT = 2;
    public static final int META_EVENT_TYPE_TRACK_NAME = 3;
    public static final int META_EVENT_TYPE_INSTRUMENT_NAME = 4;
    public static final int META_EVENT_TYPE_LYRIC = 5;
    public static final int META_EVENT_TYPE_MARKER = 6;
    public static final int META_EVENT_TYPE_CUE_POINT = 7;
    public static final int META_EVENT_TYPE_END_OF_TRACK = 0x2f;
    public static final int META_EVENT_TYPE_SET_TEMPO = 0x51;
    public static final int META_EVENT_TYPE_SET_TIMESIG = 0x58;
    public static final int META_EVENT_TYPE_SET_KEYSIG = 0x59;
    public static final int META_EVENT_TYPE_SET_SEQUENCER = 0x7f;

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
     * Get instrument (BPM)
     * @return BPM
     */
    public int getTempo(){
        if( mEventType == TYPE_META_EVENT && mMetaEventType == META_EVENT_TYPE_SET_TEMPO){
            int MPQN = (mData[0] & 0xff) << 16 | (mData[1] & 0xff) << 8 | (mData[2] & 0xff);
            int BPM = MICROSECONDS_PER_MINUTE/MPQN;
            Log.d(TAG, "MPQN=["+MPQN+"] BPM=["+BPM+"]");
            return BPM;
        }
        return 0;
    }

    /**
     * Get TimeSignatire
     * @return Beats per bar (numerator)
     */
    public int getTimeSig() {
        if( mEventType == TYPE_META_EVENT && mMetaEventType == META_EVENT_TYPE_SET_TIMESIG){
            Log.d(TAG, "TIMESIG=["+mData[0]+"]["+mData[1]+"]["+mData[2]+"]["+mData[3]+"]");
            int numerator=mData[0];
            int denominator=mData[1];
            int midiClocks=mData[2];
            int notesPer24MidiClocks=mData[2];
            Log.d(TAG, "TIMESIG:["+numerator+"/"+denominator+"] midiClocks:["+midiClocks+"]");
            return numerator;
        }
        return 0;
    }

    /**
     * Constructs a new NOTE Event
     *
     * @param ticks Ticks delay before firing this event
     * @param type What sort of note event is it
     * @param channel The midi mChannel (not used)
     * @param param1 The note
     * @param param2 Velocity (not used)
     */
    public MidiEvent(int ticks, int type, int channel, int param1, int param2) {
        this.mEventType = TYPE_NOTE_EVENT;
        this.mNoteEventType = type;
        this.mTicks = ticks;
        this.mChannel = channel;
        this.mParam1 = param1;
        this.mParam2 = param2;
    }

    public boolean isNoteOnOrOff() {
        return mEventType == TYPE_NOTE_EVENT &&
                (mNoteEventType == NOTE_EVENT_TYPE_NOTE_ON || mNoteEventType == NOTE_EVENT_TYPE_NOTE_OFF);
    }
    public boolean isEndOfTrack(){
        return mEventType == TYPE_META_EVENT &&
                mMetaEventType == META_EVENT_TYPE_END_OF_TRACK;
    }

    public int getBend() {
        if (mNoteEventType == NOTE_EVENT_TYPE_PITCHBEND) {
            int ret = ((mParam2 & 0x7F) << 7) + (mParam1 & 0x7F);
            Log.d(TAG, "getBend:[" + MidiFile.iToHex(mParam1) + "][" + MidiFile.iToHex(mParam2) + "] = " + ret);
            return ret;
        }
        return 0;
    }
    public static String metaEventType(int type){
        switch (type){
            case META_EVENT_TYPE_TRACK_SEQ:
                return "Track SEQUENCE";
            case META_EVENT_TYPE_TRACK_TEXT:
                return "Track TEXT";
            case META_EVENT_TYPE_TRACK_COPYRIGHT:
                return "Track COPYRIGHT";
            case META_EVENT_TYPE_TRACK_NAME:
                return "Track TRACK NAME";
            case META_EVENT_TYPE_INSTRUMENT_NAME:
                return "Track INSTRUMENT NAME";
            case META_EVENT_TYPE_LYRIC:
                return "Track LYRIC";
            case META_EVENT_TYPE_MARKER:
                return "Track MARKER";
            case META_EVENT_TYPE_CUE_POINT:
                return "Track CUEPOINT";
            case META_EVENT_TYPE_END_OF_TRACK:
                return "Track EOT";
            case META_EVENT_TYPE_SET_TEMPO:
                return "Track TEMPO";
            case META_EVENT_TYPE_SET_TIMESIG:
                return "Track TIMESIG";
            case META_EVENT_TYPE_SET_KEYSIG:
                return "Track KEYSIG";
            case META_EVENT_TYPE_SET_SEQUENCER:
                return "Track SEQUENCER STUFF";
            default:
                return "UNKNOWN";
        }
    }
}