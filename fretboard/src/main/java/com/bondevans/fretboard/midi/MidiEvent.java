package com.bondevans.fretboard.midi;

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
    public static final int META_EVENT_TYPE_TRACK_NAME = 3;
    public static final int META_EVENT_TYPE_INSTRUMENT_NAME = 4;
    public static final int META_EVENT_TYPE_END_OF_TRACK = 0x2f;
    public static final int META_EVENT_TYPE_SET_TEMPO = 0x51;

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