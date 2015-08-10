package com.bondevans.fretboard.fretboardplayer;

import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads and parses midi file data
 */
public class MidiFile {
    private static final int TYPE_NOTE_OFF = 8;
    private static final int TYPE_NOTE_ON = 9;
    private static final int TYPE_NOTE_AFTER_TOUCH = 10;
    private static final int TYPE_CONTROLLER = 11;
    private static final int TYPE_PROGRAM_CHANGE = 12;
    private static final int TYPE_CHANNEL_AFTERTOUCH = 13;
    private static final int TYPE_PITCHBEND = 14;
    private static final int TYPE_META_SYSEX = 16;
    private static final int TYPE_META_EVENT = 16;
    private static final int TYPE_SYSEX = 0;
    private static final String TAG = "MidiFile";

    Uri midiFile;
    int format; //  0=single track, 1=multiple tracks
    int tracks; //  Number of tracks in this file
    int ticksPerBeat;
    String[] trackName;
    int[] trackChunkLength;
    private List<MidiEvent> midiEvents = new ArrayList<>();


    MidiFile(Uri midiFile) {
        // Open up file and load header and track details
        this.midiFile = midiFile;

        // TODO read header
        this.format = 0;
        this.tracks = 1;
        this.ticksPerBeat = 48;
        this.trackName = new String[tracks];
        this.trackChunkLength = new int[tracks];

        // TODO - Get names of each track
        this.trackName[0] = "Guitar";
        this.trackChunkLength[0] = 9;
    }

    /**
     * Loads the specified track and into an array of MidiEvents
     *
     * @param track
     */
    byte[] loadBytes(int track) {
        byte[] trackBytes = new byte[trackChunkLength[track]];
        // TODO - Read track from file
        trackBytes = new byte[]{(byte) 0x00, (byte) 0xff, (byte) 0x03, (byte) 0x05, (byte) 0x50, (byte) 0x69, (byte) 0x61, (byte) 0x6E, (byte) 0x6F};// TODO - REMOVE ME
        return trackBytes;
    }

    MidiEvent[] loadFretEvents(int track) {
        byte[] trackByte = loadBytes(track);
        // Read through each event - discard any events we are not interested in  - only interested in note on and off
        // Need to ensure that we keep a track of the delta times of discarded events
        int i = 0;
        byte eventType;
        int channel;
        int len = 0;
        int note;
        int delta = 0;

        while (i < trackByte.length) {
            // 1st variable bytes (1-4) indicates delta time
            int loop = 0;
            while ((trackByte[i] & 0x80) > 0) {
                delta += trackByte[i++] & 0x7F << (loop++ * 7);
                Log.d(TAG, "delta=[" + delta + "]");
                ++i;
            }
            ++i;
            // 2nd byte top 4 bits indicates type (low 4 bits = channel)
            eventType = trackByte[i++];

            // Work out whether its a meta event or a sysex from the bottom 4 bits of the type byte
            if (eventType == (byte) 0xff) {
                // META EVENT Work out variable length of the meta event
                i++;// Dont care about type
                loop = 0;
                while ((trackByte[i] & 0x80) > 0) {
                    len += trackByte[i++] & 0x7F << (loop++ * 7);
                    Log.d(TAG, "length=[" + delta + "]");
                    ++i;
                }

                i += len; // Ignore the actual data - not interested
            } else if (eventType == (byte) 0xf0) {
                Log.d(TAG, "SYSEX - OOPS TODO TODO TODO");
            } else {
                // Must be a midi event
                byte midiEventType = (byte) (eventType & 0xf);
                switch (midiEventType) {
                    case TYPE_NOTE_OFF:
                    case TYPE_NOTE_ON:
                        Log.d(TAG, "NOTE_OFF/ON");
                        // 1st byte is note, 2nd byte is velocity (ignored)
                        note = trackByte[i];
                        i += 3;
                        midiEvents.add(new MidiEvent(note, eventType, delta));
                        break;
                    case TYPE_NOTE_AFTER_TOUCH:
                    case TYPE_CONTROLLER:
                        Log.d(TAG, "NOTE_AFTER_TOUCH/CONTROLLER");
                        i += 2;
                        break;
                    case TYPE_PROGRAM_CHANGE:
                    case TYPE_CHANNEL_AFTERTOUCH:
                        Log.d(TAG, "PROGRAM_CHANGE/AFTERTOUCH");
                        i++;
                        break;
                    case TYPE_PITCHBEND:
                        Log.d(TAG, "PICTHBEND");
                        i += 2;
                        break;
                    case TYPE_META_SYSEX:

                    default:
                        Log.d(TAG, "OOPS - SOMETHING WENT WRONG [" + eventType + "]");
                }
            }
            return (MidiEvent[]) midiEvents.toArray();
        }
    }
