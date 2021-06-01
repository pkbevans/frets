package com.bondevans.frets.fretviewer;

import android.media.midi.MidiReceiver;
import android.os.Handler;
import android.util.Log;

import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.fretview.FretPlayerEvent;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.midi.Midi;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

import static java.lang.Thread.sleep;

public class FretPlayer {

    private static final String TAG = FretPlayer.class.getSimpleName();
    private static final int DEFAULT_TPQN = 480;
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
    public void setTrack(MidiReceiver midiReceiver, int tpqn, int tempo, int currentFretEvent) {
        mMidiReceiver = midiReceiver;
        mTempo = tempo;
        mCurrentEvent = currentFretEvent;
        mTicksPerQtrNote = tpqn>0?tpqn:DEFAULT_TPQN;
        mPlaying = false;
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
                    mFretPlayerEvent = mFretSong.mFretPlayerEvents.get(mCurrentEvent);
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
                    if (++mCurrentEvent >= mFretSong.mFretPlayerEvents.size()) {
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
        for(FretPlayerEvent fretPlayerEvent : mFretSong.mFretPlayerEvents){
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