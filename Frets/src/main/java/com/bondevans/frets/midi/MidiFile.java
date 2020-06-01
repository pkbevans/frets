package com.bondevans.frets.midi;

import android.util.Log;

import com.bondevans.frets.exception.FretboardException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
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
    private static final String UNKNOWN_TRACKNAME = "<UNKNOWN>";
    private static final int DEFAULT_TEMPO = 120;
    private static final int BEND_MIN_TICKS = 40;
    private static final int DEFAULT_TIMESIG = 4;

    private File mMidiFile;
    private String songTitle;
    private List<MidiTrack> mTracks;
    private int mTicksPerQtrNote;
    private int BPM=120;
    private int[] mTrackChunkLength;
    private boolean mHeaderLoaded;
    private int mRunningStatus = 0;
    private int mRunningChannel = 0;
    private int mTotalTicks;

    MidiFile(File midiFile) throws FretboardException {
        // Open up file and load header and track details
        this.mMidiFile = midiFile;

        loadHeader(mMidiFile);
        mHeaderLoaded = true;
    }

    List<MidiTrack> getTracks() throws FretboardException {
        if (!mHeaderLoaded) {
            throw new FretboardException("ERRROR - Header not loaded");
        }
        return mTracks;
    }

    /**
     * Loads the specified track into a byte array
     *
     * @param track Track number (1st = 0)
     * @return Returns byte buffer containing track mData
     */
    private byte[] loadTrack(int track) throws FretboardException {
        Log.d(TAG, "setTrack [" + track + "]");
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
            in = new BufferedInputStream(new FileInputStream(mMidiFile), BUF_LEN);
        } catch (FileNotFoundException e) {
            throw new FretboardException("ERROR-reading track");
        }
        try {
            if (in.skip(skip) != skip) {
                throw new FretboardException("ERROR-skipping");
            }
            if (in.read(buffer, 0, mTrackChunkLength[t]) != mTrackChunkLength[t]) {
                throw new FretboardException("ERROR-reading track");
            }
            in.close();
        } catch (IOException e) {
            throw new FretboardException(e.getMessage());
        }
        return buffer;
    }

    /**
     * Loads a track into a List of MidiNoteEvents
     * <p/>
     * we are not interested in, in a 2nd step.  Would make debugging easier....
     *
     * @param track Track number to load
     * @throws FretboardException FretboardException
     * @throws IOException IOException
     */
    void loadNoteEvents(int track, List<MidiNoteEvent> noteEvents) throws FretboardException, IOException {
        Log.d(TAG, "loadNoteEvents for track:" + track);
        InputStream in = new ByteArrayInputStream(loadTrack(track));
        MidiEvent ev = getEvent(in);
        int runningTicks = 0;
        mTotalTicks=0;

        while (!ev.isEndOfTrack()) {
            if (ev.isNoteOnOrOff()) {
                // If velocity is zero then this effectively a FretNote Off message, so we'll store it as such
                if(ev.mParam2 == 0 && MidiEvent.NOTE_EVENT_TYPE_NOTE_ON == ev.mNoteEventType){
                    ev.mNoteEventType = MidiEvent.NOTE_EVENT_TYPE_NOTE_OFF;
                }
                MidiNoteEvent xxx = new MidiNoteEvent(ev.mParam1, ev.mNoteEventType == MidiEvent.NOTE_EVENT_TYPE_NOTE_ON, ev.mTicks + runningTicks);
//                Log.d(TAG, "HELLO GOT NOTE EVENT,"+xxx.deltaTicks+","+xxx.note+","+(xxx.on?"ON":"OFF"));
                noteEvents.add(xxx);
                runningTicks = 0;
            }
            else if (ev.mNoteEventType == MidiEvent.NOTE_EVENT_TYPE_PITCHBEND) {
                // Dont add too many consecutive bend events in a short space of time
                if(noteEvents.size()>0 && noteEvents.get(noteEvents.size()-1).type== MidiNoteEvent.TYPE_BEND &&
                        (ev.mTicks+runningTicks)<BEND_MIN_TICKS){
                    // Ignore this bend event - but add the delta on to the next one
                    Log.d(TAG, "ignoring bend:"+ev.mTicks);
                    runningTicks += ev.mTicks;
                }else {
                    noteEvents.add(new MidiNoteEvent(MidiNoteEvent.TYPE_BEND, ev.mTicks + runningTicks, ev.getBend()));
                    runningTicks = 0;
                }
            }
            else if(ev.mMetaEventType == MidiEvent.META_EVENT_TYPE_SET_TEMPO) {
                Log.d(TAG, "TEMPO="+ev.getTempo());
                noteEvents.add(new MidiNoteEvent(MidiNoteEvent.TYPE_TEMPO, ev.mTicks + runningTicks, ev.getTempo()));
                runningTicks=0;
            }
            else
            {
                // need to keep running total of mTicks for events that we ignore.
                runningTicks += ev.mTicks;
            }
            ev = getEvent(in);
        }
        Log.d(TAG, "loadNoteEvents (end) got " + noteEvents.size() + " events for track:" + track);
    }

    /**
     * Gets the next event in the midi track
     * @param in Track InputStream
     * @return MidiEvent
     * @throws IOException in the event of a file read failure
     */
    private MidiEvent getEvent(InputStream in) throws IOException {
        int param1, param2 = 0;
        int channel;
        int ticks = getTimeTicks(in);
        mTotalTicks+=ticks;
//        Log.d(TAG, "TICKS["+ticks+"]");
        int type = in.read();
        // What sort of event is it?
        if (type == 0xff) {
//            Log.d(TAG, "TYPE["+iToHex(type)+"]");
            int len;
            // Meta event (or END OF TRACK)
            int metaType = in.read();
            len = getVariableLen(in);
            if (metaType == MidiEvent.META_EVENT_TYPE_END_OF_TRACK) {
                Log.d(TAG, "END OF TRACK");
            }
            else if (metaType == MidiEvent.META_EVENT_TYPE_SET_TEMPO) {
                Log.d(TAG, "SET TEMPO"+ " mLen["+len+"]");
            }
            else if (metaType == MidiEvent.META_EVENT_TYPE_SET_TIMESIG) {
                Log.d(TAG, "SET TIMESIG :"+ " mLen["+len+"]");
            }
            else
            {
                Log.d(TAG, "META EVENT=mNoteEventType[" + metaType + "] mLen["+len+"]: "+MidiEvent.metaEventType(metaType));
            }
            return new MidiEvent(MidiEvent.TYPE_META_EVENT, metaType, len, in);
        } else if (type == 0xf0) {
//            Log.d(TAG, "TYPE["+iToHex(type)+"]");
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
//                Log.d(TAG, "Running status TYPE [" + noteEventType + "]");
            } else {
                // mChannel = bottom 4 bits
                channel = type & 0x0f;
                param1 = in.read();
            }
//            Log.d(TAG, "TYPE: "+iToHex(type)+"=>"+noteEventType+channel);

            switch (noteEventType) {
                case MidiEvent.NOTE_EVENT_TYPE_NOTE_OFF:
                case MidiEvent.NOTE_EVENT_TYPE_NOTE_ON:
                    // 1st byte is note, 2nd byte is velocity
                    param2 = in.read();
                    Log.d(TAG, "HELLO NOTE: "+ticks + " "+ iToHex(type)+"=>"+noteEventType+channel
                            +" "+ iToHex(param1)+" "+iToHex(param2)+" "+noteName(param1)+ " "+(param2==0?"OFF":"ON")+ " TotTICKS: "+mTotalTicks);
                    break;
                case MidiEvent.NOTE_EVENT_TYPE_NOTE_AFTER_TOUCH:
                case MidiEvent.NOTE_EVENT_TYPE_CONTROLLER:
                case MidiEvent.NOTE_EVENT_TYPE_PITCHBEND:
                    param2 = in.read();
                    if (noteEventType == MidiEvent.NOTE_EVENT_TYPE_PITCHBEND) {
                        Log.d(TAG, "PICTHBEND [" + noteEventType + "][" + iToHex(param1) + "][" + iToHex(param2) + "]");
                    } else {
                        Log.d(TAG, "NOTE_AFTER_TOUCH/CONTROLLER [" + noteEventType + "][" + iToHex(param1) + "][" + iToHex(param2) + "]");
                    }
                    break;
                case MidiEvent.NOTE_EVENT_TYPE_PROGRAM_CHANGE:
                    Log.d(TAG, "PROGRAM_CHANGE [" + noteEventType + "]["+iToHex(param1)+"]");
                    break;
                case MidiEvent.NOTE_EVENT_TYPE_CHANNEL_AFTERTOUCH:
                    Log.d(TAG, "AFTERTOUCH [" + noteEventType + "]["+iToHex(param1)+"]");
                    break;
                default:
                    Log.e(TAG, "OOPS - SOMETHING WENT WRONG [" + noteEventType + "]");
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
    public static String noteName(int note) {
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
     * @throws IOException in the event of a file read failure
     */
    private int getTimeTicks(InputStream in) throws IOException {
        int ticks=0;
        int trackByte = in.read();
        StringBuilder sb = new StringBuilder("TICKS: "+iToHex(trackByte));
//        Log.d(TAG, "HELLO NOTE (TICK BYTE)," + iToHex(trackByte));
        while ((trackByte & 0x80) > 0) {
            ticks = (ticks<<7) | (trackByte & 0x7F);
            trackByte = in.read();
//            Log.d(TAG, "HELLO NOTE (TICK BYTE)," + iToHex(trackByte));
            sb.append(" ").append(iToHex(trackByte));
        }
        ticks = (ticks<<7) | (trackByte & 0x7F);
        sb.append(" ").append(ticks);
        Log.d(TAG, sb.toString());
        return ticks;
    }

    /**
     * Load midi file header and set up the track names and lengths
     *
     * @param file Path to midi file
     * @throws FretboardException if an error occurs
     */
    private void loadHeader(File file) throws FretboardException {
        Log.d(TAG, "loadHeader");
        BufferedInputStream in;
        byte[] buffer = new byte[BUF_LEN];
        int format; //  0=single track, 1=multiple Tracks
        int tracks; //  Number of tracks in this file

        // Open up the file and read the header
        try {
            in = new BufferedInputStream(new FileInputStream(file), BUF_LEN);
            if (in.read(buffer, 0, FILE_HEADER_LENGTH) != FILE_HEADER_LENGTH) {
                //error - abort
                throw new FretboardException("Error - Reading file header");
            }
            format = buffer[9] & 0xFF;
            tracks = buffer[11] & 0xFF;
            if( (buffer[12] & 0x80) == 0){
                // Time division is ticks per beat
                this.mTicksPerQtrNote = (buffer[12] << 8)+ (buffer[13] & 0xFF);
            }
            else{
                // Time division is frames per second
                Log.e(TAG, "OOPS - time division is frames per sec");
                throw new FretboardException("OOPS - time division is frames per sec");
            }
            Log.d(TAG, "format=" + format + " tracks=" + tracks + " ticksPerQtrBeat=" + mTicksPerQtrNote);

            this.mTracks = new ArrayList<>();
            this.mTrackChunkLength = new int[tracks];

            // For each track
            // Now get the track details
            int track = 0;
            while (track < tracks) {
                // read track header
                if (in.read(buffer, 0, TRACK_HEADER_LENGTH) != TRACK_HEADER_LENGTH) {
                    throw new FretboardException("Error - Reading track header");
                }
                int trackLen = ((buffer[4] & 0xFF) << 24) + ((buffer[5] & 0xFF) << 16) + ((buffer[6] & 0xFF) << 8) + ((buffer[7] & 0xFF));
                Log.d(TAG, "Track: " + track + " mLen: " + trackLen);
                String trackName = getTrackName(in);
                Log.d(TAG, "Adding track: " + trackName);
                if(track==0) {
                    // If this is the first track then assume that we have got the song title
                    this.songTitle = trackName;
                }
                mTracks.add(new MidiTrack(trackName, track));
                if(track==0){
                    // Lets get the tempo from the first track.
                    this.BPM = getTempoNew(in);
                }
                mTrackChunkLength[track] = trackLen;
                // SKIP track mData
                while(trackLen>0) {
                    long skipped = in.skip(trackLen);
                    if(skipped<0){
                        throw new FretboardException("Error - Skipping track data ("+trackLen+")");
                    }
                    trackLen-=skipped;
                }
                ++track;
            }
            in.close();
        }catch (IOException e) {
            Log.d(TAG, "IOException: "+e.getMessage());
            throw new FretboardException(e.getMessage());
        }
    }

    /**
     * Searches for a Trackname meta-event in the given InputStream
     *
     * @param in BufferedInputStream containing track mData
     * @return Track name or instrument name
     * @throws IOException in the event of a file io error
     */
    private String getTrackName(BufferedInputStream in) throws IOException {
        Log.d(TAG, "getTrackName");
        // Get the first event
        String ret="";
        in.mark(BUF_LEN);
        MidiEvent ev = getEvent(in);
        int count = 1;
        // Look for the track name event but give up after 10 events
        while (!ev.isEndOfTrack() && ret.isEmpty() && count <= 10) {
            if (ev.mEventType == MidiEvent.TYPE_META_EVENT &&
                    (ev.mMetaEventType == MidiEvent.META_EVENT_TYPE_TRACK_NAME ||
                            ev.mMetaEventType == MidiEvent.META_EVENT_TYPE_INSTRUMENT_NAME)) {
                ret = ev.getMetaDataString();
            }
            // Get the next event
            ev = getEvent(in);
            ++count;
        }
        in.reset();
        return (ret.isEmpty()?UNKNOWN_TRACKNAME: ret);
    }

    private int getTempo(BufferedInputStream in) throws IOException {
        Log.d(TAG, "getTempo");
        // Get the first event
        int ret=0;
        in.mark(BUF_LEN);
        MidiEvent ev = getEvent(in);
        int count = 1;
        while (!ev.isEndOfTrack() && ret==0 && count <=10) {
            if (ev.mEventType == MidiEvent.TYPE_META_EVENT &&
                    (ev.mMetaEventType == MidiEvent.META_EVENT_TYPE_SET_TEMPO)) {
                ret = ev.getTempo();
            }
            // Get the next event
            ev = getEvent(in);
            ++count;
        }
        in.reset();
        return (ret==0?DEFAULT_TEMPO:ret);
    }

    private int getTempoNew(BufferedInputStream in) throws IOException {
        MidiEvent ev = getMetaEventByType(in, MidiEvent.META_EVENT_TYPE_SET_TEMPO);
        return ev.getTempo()==0?DEFAULT_TEMPO:ev.getTempo();
    }
    private int getTimeSig(BufferedInputStream in) throws IOException {
        MidiEvent ev = getMetaEventByType(in, MidiEvent.META_EVENT_TYPE_SET_TIMESIG);
        return ev.getTimeSig()==0?DEFAULT_TIMESIG:ev.getTimeSig();
    }
    private MidiEvent getMetaEventByType(BufferedInputStream in, int eventType) throws IOException {
        Log.d(TAG, "getMetaEventByType");
        // Get the first event
        in.mark(BUF_LEN);
        MidiEvent ev = getEvent(in);
        int count = 1;
        while (!ev.isEndOfTrack() && count <=10) {
            if (ev.mEventType == MidiEvent.TYPE_META_EVENT &&
                    (ev.mMetaEventType == eventType)) {
                in.reset();
                return ev;
            }
            // Get the next event
            ev = getEvent(in);
            ++count;
        }
        in.reset();
        return ev;
    }

    /**
     * Convert Int to a hex string
     * @param in Integer
     * @return hex string
     */
    static String iToHex(int in){
        String hex = Integer.toHexString(in);
        if( hex.length()<2){
            hex = "0" + hex;
        }
        return hex;
    }

    String getSongTitle() {
        return songTitle;
    }

    int getTicksPerQtrNote() {
        return mTicksPerQtrNote;
    }

    int getBPM() {
        return BPM;
    }
}
