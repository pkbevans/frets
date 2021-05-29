package com.bondevans.frets.fretviewer;

import android.media.midi.MidiReceiver;
import android.os.Handler;
import android.util.Log;

import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.midi.Midi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static java.lang.Thread.sleep;

public class FretPlayer {

    private static final String TAG = FretPlayer.class.getSimpleName();
    private static final int DEFAULT_TPQN = 480;
    private static final int MIN_BEND_TICKS = 30;
    private final Executor executor;
    private MidiReceiver mMidiReceiver;
    private final OnUiUpdateRequiredListener onUiUpdateRequiredListener;
    private final Handler handler;
    private boolean mPlaying;
    private int mTicksPerQtrNote;
    private int mTempo;
    private final byte[] midiBuffer = new byte[3];
    private static final int MIDI_CHANNEL_DRUMS = 9;
    private final FretSong mFretSong;
    private FretTrack mFretTrack;
    private int mSoloTrack;
    private FretEvent prevEvent = new FretEvent(0,0,0);
    List<FretPlayerEvent> mFretPlayerEvents;
    List<FretNote> uiNotes;
    int mCurrentEvent=0;
    FretPlayerEvent mFretPlayerEvent;
    int delayMill;

    public interface OnUiUpdateRequiredListener {
        void onClickEvent(int clickEvent, int currentEvent);
        void onSoloTrackNoteEvent(List<FretNote> fretNotes, int bend);
        void onTempoChange(int newTempo);
    }
    public FretPlayer(OnUiUpdateRequiredListener onUiUpdateRequiredListener,
                      Handler handler,
                      Executor executor,
                      FretSong fretSong) {
        this.onUiUpdateRequiredListener = onUiUpdateRequiredListener;
        this.handler = handler;
        this.executor = executor;
        this.mFretSong = fretSong;
    }
    public void setTrack(MidiReceiver midiReceiver, FretTrack frettrack, int tpqn, int tempo, int currentFretEvent, int soloTrack) {
        mMidiReceiver = midiReceiver;
        mFretTrack = frettrack;
        mTempo = tempo;
        mCurrentEvent = currentFretEvent;
        mTicksPerQtrNote = tpqn>0?tpqn:DEFAULT_TPQN;
        mSoloTrack = soloTrack;
        mPlaying = false;
        buildTrack();
    }
    void buildTrack() {
        byte[] buffer = new byte[0];
        int channel;
        int bend;
        int delay;
        int ticks = 0;
        if(null== mFretPlayerEvents) {
            mFretPlayerEvents = new ArrayList<>();
            for (FretEvent fretEvent : mFretTrack.fretEvents) {
                Log.d(TAG, "HELLO Building: "+fretEvent.toLogString());
                delay = calcDelay(fretEvent.deltaTicks);
                if (fretEvent.fretNotes.size()> 0 || fretEvent.bend > 0) {
                    // Build the midi buffer
                    buffer = new byte[(fretEvent.fretNotes.size() + (fretEvent.bend>0?1:0)) * 3];
                    int i = 0;
                    channel = mFretSong.getTrack(fretEvent.track).isDrumTrack() ? MIDI_CHANNEL_DRUMS : fretEvent.track;
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
                }
                // Filter out some bend events for the UI and any notes NOT on the solo track
                if((fretEvent.bend>0 && prevEvent.bend>0 && ticks < MIN_BEND_TICKS) || mSoloTrack != fretEvent.track ){
                    // Ignore if this is a bender and prev event was a bender, and ticks < MIN_BEND_TICKS
                    bend = 0;
                    uiNotes= new ArrayList<>();
                    Log.d(TAG, "HELLO Ig1: " + fretEvent.toLogString());
                    ticks+=fretEvent.deltaTicks;
                } else {
                    bend = fretEvent.bend;
                    uiNotes = fretEvent.fretNotes;
                    prevEvent = fretEvent;
                    ticks = 0;
                }
                mFretPlayerEvents.add(new FretPlayerEvent(fretEvent.getTicks(), delay, buffer, fretEvent.getClickEvent(), fretEvent.track, bend, uiNotes));
            }
            // REMOVE
            int x=1;
            for(FretPlayerEvent fretPlayerEvent : mFretPlayerEvents){
                Log.d(TAG, "HELLO Built: "+(x++)+" "+ fretPlayerEvent.toString());
            }
            // REMOVE
        }
    }
    /**********************************************************************************************/
    /* Interfaces for UI to communicate with Midi Player
    /**********************************************************************************************/
    public void setPlaying(boolean playing) {
//        Log.d(TAG, "HELLO - toggling play/pause");
        mPlaying = playing;
        if (mPlaying) {
            executor.execute(() -> {
                setMidiInstruments();
                while (mPlaying) {
                    mFretPlayerEvent = mFretPlayerEvents.get(mCurrentEvent);
//                    Log.d(TAG, "HELLO midi event: " + mCurrentEvent + " " + mMidiNoteBuffer.toString());
                    try {
                        sleep(mFretPlayerEvent.delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mFretPlayerEvent.midiBuffer.length > 0) {
                        midiSend(mFretPlayerEvent.midiBuffer, mFretPlayerEvent.midiBuffer.length, System.nanoTime());
                    }
                    if (mFretPlayerEvent.click > 0) {
                        // Update progress listener (so it can update the seekbar)
                        clickEvent(mFretPlayerEvent.click, mCurrentEvent);
                    }
                    if (mFretPlayerEvent.uiFretNotes.size() > 0 || mFretPlayerEvent.bend > 0) {
                        noteEvent(mFretPlayerEvent.uiFretNotes, mFretPlayerEvent.bend);
                    }
                    // Loop round to start
                    if (++mCurrentEvent >= mFretPlayerEvents.size()) {
                        mCurrentEvent = 0;
                    }
                }
            });
        }else{
            // Pause
            sendMidiNotesOff();
        }
    }
    public void setTempo(int tempo) {
        mTempo = tempo;
        // Recalculate delays
        for(FretPlayerEvent fretPlayerEvent : mFretPlayerEvents){
            fretPlayerEvent.delay = calcDelay(fretPlayerEvent.ticks);
        }
    }
    public void moveTo(int event) {
        mCurrentEvent = event;
    }
    /**********************************************************************************************/
    /* Callbacks for Midi Player to communicate with UI
    /**********************************************************************************************/
    private void tempoChange(final int tempo) {
        handler.post(() -> onUiUpdateRequiredListener.onTempoChange(tempo));
    }
    private void clickEvent(final int clickEvent, final int currentEvent) {
        handler.post(() -> onUiUpdateRequiredListener.onClickEvent(clickEvent, currentEvent));
    }
    private void noteEvent(final List<FretNote> fretNotes, final int bend) {
        handler.post(() -> onUiUpdateRequiredListener.onSoloTrackNoteEvent(fretNotes, bend));
    }
    /**
     * Sends NOTE_OFF message for all 128 notes on all channels used.
     */
    private void sendMidiNotesOff() {
        Log.d(TAG, "HELLO Sending NEW ALL NOTES OFF");
        for (int channel = 0; channel < mFretSong.tracks(); channel++) {
            midiBuffer[0] = (byte) (Midi.ALL_NOTES_OFF | (mFretSong.getTrack(channel).isDrumTrack() ? MIDI_CHANNEL_DRUMS : channel));
            midiBuffer[1] = (byte) 120;
            midiBuffer[2] = (byte) 0x00;
            midiSend(midiBuffer, 3, System.nanoTime());
//            for (int noteValue = 0; noteValue < 128; noteValue++) {
//                midiBuffer[0] = (byte) (Midi.NOTE_OFF | (mFretSong.getTrack(channel).isDrumTrack() ? MIDI_CHANNEL_DRUMS : channel));
                // Note value
//                midiBuffer[1] = (byte) noteValue;
                // Velocity - ZERO volume
//                midiBuffer[2] = (byte) 0x00;
//                midiSend(midiBuffer, 3, System.nanoTime());
//            }
        }
    }
    private void midiSend(byte[] buffer, int count, long timestamp) {
        try {
            // send event immediately
            mMidiReceiver.send(buffer, 0, count, timestamp);
        } catch (IOException e) {
            Log.e(TAG, "HELLO midiSend failed " + e.getMessage());
        }
    }

    private void setMidiInstruments() {
        Log.d(TAG, "setMidiInstruments");
        for (int track = 0; track < mFretSong.tracks(); track++) {
            FretTrack fretTrack = mFretSong.getTrack(track);
            if (!fretTrack.isDrumTrack() && !fretTrack.isClickTrack()) {
                // No instrument for drum track - just channel 10.  No instrument for click track
                setMidiInstrument(track, fretTrack.getMidiInstrument());
            }
        }
    }
    private void setMidiInstrument(int channel, int instrument) {
        byte[] event = new byte[2];
        event[0] = (byte) (Midi.SET_INSTRUMENT | channel);
        event[1] = (byte) instrument;
        midiSend(event, 2, System.nanoTime());
    }
    static class FretPlayerEvent {
        long ticks;
        int delay;
        byte [] midiBuffer;
        int click;
        int track;
        int bend;
        List <FretNote> uiFretNotes;

        public FretPlayerEvent(long ticks, int delay, byte[] midiBuffer, int click, int track, int bend, List <FretNote> fretNotes) {
            this.ticks = ticks;
            this.delay = delay;
            this.midiBuffer = midiBuffer;
            this.click = click;
            this.track = track;
            this.bend = bend;
            this.uiFretNotes = fretNotes;
//            Log.d(TAG, "HELLO Added: "+toString());
        }
        public String toString(){
            return "ticks:"+ticks+" delay:"+delay+" click:"+click+" track:"+track+" bend:"+bend+" uiFretNotes:"+ uiFretNotes.size();
        }
    }
    private int calcDelay(long deltaTicks) {
        delayMill=0;
        if (deltaTicks > 0) {
            // Avoid divide by zero error
            double x = ((60 * 1000) / mTempo);  // Tempo is BPM. x = number of mill-secs per beat
            double y = x / mTicksPerQtrNote;    // y = how many milliseconds per tick
            double z = deltaTicks * y;          // z = how many milliseconds for given number of ticks
            delayMill = (int) z;
        }
        return delayMill;
    }
}