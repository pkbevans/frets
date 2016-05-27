package com.bondevans.frets.fretview;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**¬
 * <code>FretTrackView</code> extends <code>FretView</code>  to animate the notes of a song on a fretboard
 */
public class FretTrackView extends FretView {
    private static final String TAG = FretTrackView.class.getSimpleName();
    private static final int MINIMUM_TEMPO = 10;
    private static final int MIDI_CHANNEL_DRUMS = 9;
    private static final int MIDI_CHANNEL_0 = 0;
    private FretTrack mFretTrack;
    private int mTicksPerQtrNote;
    private int mDefaultTempo = 120;
    private int mCurrentBPM;
    private boolean mPlaying = false;
    private GestureDetector gestureDetector;
    private FretEventHandler mFretEventHandler;
    private int mCurrentFretEvent = 0;
    int mChannel;
    private FretListener fretListener;

    public interface FretListener {
        void OnProgressUpdated(int numberEvents, int currentEvent);

        void OnTempoChanged(int tempo);

        void OnPlayEnabled();

        void SendMidi(byte[] buffer);
    }

    public void setFretListener(FretListener fretListener) {
        this.fretListener = fretListener;
    }

    public FretTrackView(Context context) {
        super(context);
        initialiseView(context);
    }

    /**
     * Constructor
     */
    public FretTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseView(context);
    }

    /**
     * All initialisation stuff
     *
     * @param context Context
     */
    private void initialiseView(Context context) {
        super.initialiseView();
        gestureDetector = new GestureDetector(context, new MyGestureListener());
        OnTouchListener gestureListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        this.setOnTouchListener(gestureListener);
        mFretEventHandler = new FretEventHandler();
    }

    @Override
    protected void onDraw(Canvas g) {
        super.onDraw(g);
        doTempo();
        if (mPlaying) {
            sendMidiNotes();
        }
    }

    private void doTempo() {
        if (mCurrentBPM == 0) {
            mCurrentBPM = mDefaultTempo;  // Set it to default instrument first time
        }
    }

    private void sendMidiNotes() {
        if (mFretNotes != null) {
            for (int i = 0; i < mFretNotes.size(); i++) {
                // Send the MIDI notes
                sendMidiNote(mFretNotes.get(i));
            }
        }
    }

    /**
     * Instructs the view to start loading up the specified track in the FretSong
     *
     * @param fretTrack Track to load
     * @param tpqn      ticks per quarter note
     * @param bpm       BPM
     */
    public void setTrack(FretTrack fretTrack, int tpqn, int bpm) {
        setTrack(fretTrack, tpqn, bpm, 0);
    }

    public void setTrack(FretTrack frettrack, int tpqn, int bpm, int currentFretEvent) {
        Log.d(TAG, "setTrack");
        mFretTrack = frettrack;
        mChannel = mFretTrack.isDrumTrack()?MIDI_CHANNEL_DRUMS:MIDI_CHANNEL_0;
        mCurrentBPM = bpm;
        mCurrentFretEvent = currentFretEvent;
        setNotes(mFretTrack.fretEvents.get(mCurrentFretEvent).fretNotes, mFretTrack.fretEvents.get(mCurrentFretEvent).bend);
        mTicksPerQtrNote = tpqn;
        // Enable Play
        fretListener.OnPlayEnabled();
        fretListener.OnTempoChanged(mCurrentBPM);
        invalidate();   // Force redraw
    }

    /**
     * Moves to new position in the midi event list - in response to user moving the seek bar
     *
     * @param progress - percentage through the song
     */
    public void moveTo(int progress) {
        mCurrentFretEvent = (mFretTrack.fretEvents.size() * progress / 100) - 1;
        if (mCurrentFretEvent < 0) {
            mCurrentFretEvent = 0;
        }
        Log.d(TAG, "mCurrentFretEvent [" + mCurrentFretEvent + "]");
    }

    private byte[] midiBuffer = new byte[3];

    private void sendMidiNote(FretNote fretNote) {
//            Log.d(TAG, (fretNote.on?"NOTE ON": "NOTE OFF")+ " ["+fretNote.name+"]");
        if (fretNote.bend > 0) {
            // Send pitch Bend message (will alter current note playing)
            midiBuffer[0] = (byte) (0xE0 | mChannel);
            setMidiPitchBendEvent(fretNote.bend, midiBuffer);
        } else {
            midiBuffer[0] = (byte) (fretNote.on ? (0x90 | mChannel) : (0x80 | mChannel));
            // Note value
            midiBuffer[1] = (byte) fretNote.note;
            // Velocity - Hardcoded volume for NOTE_ON and zero for NOTE_OFF
            midiBuffer[2] = (byte) (fretNote.on ? 0x60 : 0x00);
        }
        fretListener.SendMidi(midiBuffer);
    }

    /**
     * Sends NOTE_OFF message for all current notes.
     */
    private void sendMidiNotesOff() {
        Log.d(TAG, "Sending NEW ALL NOTES OFF");
        for (FretNote fretNote : mFretNotes) {
            midiBuffer[0] = (byte) (fretNote.on ? (0x90 | mChannel) : (0x80 | mChannel));
            // Note value
            midiBuffer[1] = (byte) fretNote.note;
            // Velocity - Hardcoded volume
            midiBuffer[2] = (byte) 0x00;
            fretListener.SendMidi(midiBuffer);
        }
    }

    /**
     * Converts bend value (0-10) to Midi pitch bend value in range 8192-16383
     *
     * @param bend Bend (UP) value in range 0-10
     * @param midiBuffer byte buffer to place midi bend event into
     */
    private void setMidiPitchBendEvent(int bend, byte[] midiBuffer) {
        midiBuffer[0] = (byte) (0xE0 | mChannel);
        midiBuffer[1] = (byte) (bend & 0x7F);
        midiBuffer[2] = (byte) ((byte) (bend >> 7) & 0x7F);
    }

    class FretEventHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
//            Log.d(TAG, "handleMessage fretEvent[" + mCurrentFretEvent + "]");
            if (mPlaying) {
                // Handle event
                handleEvent(mFretTrack.fretEvents.get(mCurrentFretEvent));
            }
        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }

    private void handleEvent(FretEvent fretEvent) {
        // Send next set of notes to Fretboard View
        if (fretEvent.tempo > 0) {
            mDefaultTempo = fretEvent.tempo;
        }
        setNotes(fretEvent.fretNotes, fretEvent.bend);
        // Force redraw
        invalidate();
        if (++mCurrentFretEvent >= mFretTrack.fretEvents.size()) {
            mCurrentFretEvent = 0;
        }
        long delay = delayFromClicks(mFretTrack.fretEvents.get(mCurrentFretEvent).deltaTime);

        // Update progress listener (so it can update the seekbar (or whatever)
        fretListener.OnProgressUpdated(mFretTrack.fretEvents.size(), mCurrentFretEvent);
        mFretEventHandler.sleep(delay);
    }

    /**
     * Plays/pauses the current track
     */
    public void play() {
        // toggle play/pause
        mPlaying = !mPlaying;
        if (mPlaying) {
            // Play
            mFretEventHandler.sleep(mFretTrack.fretEvents.get(mCurrentFretEvent).deltaTime);
        } else {
            // Pause
            sendMidiNotesOff();
        }
        invalidate();   // Force redraw with correct play/pause button
    }

    /**
     * Calculates the time between FretEvents from the delta time (clicks) and the mTicks per beat setting
     * in the file
     *
     * @param deltaTicks The delta time in ticks for this event
     * @return Returns the actual delay in millisecs for this event
     */
    private long delayFromClicks(int deltaTicks) {
        long ret = 0;

        if (mTicksPerQtrNote > 0 && deltaTicks > 0) {
            // Avoid divide by zero error
            double x = ((60 * 1000) / mCurrentBPM);
            double y = x / mTicksPerQtrNote;
            double z = deltaTicks * y;
            ret = (long) z;
        }

        return ret;
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//            Log.d(TAG, "HELLO onFling");
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//            Log.d(TAG, "HELLO onScroll distanceX=" + distanceX + "] distanceY=[" + distanceY + "]");
            if (Math.abs(distanceX) < Math.abs(distanceY)) {
                if (distanceY > 0) {
                    Log.d(TAG, "UP = SPEED UP");
                } else {
                    Log.d(TAG, "DOWN = SLOW DOWN");
                }
                updateTempo(distanceY);
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            // Need to return true else onSingleTapConfirmed() doesn't work
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "LONG PRESS");
            super.onLongPress(e);
        }
    }

    private static final int DISTANCE_FACTOR = 5;

    private void updateTempo(float distanceY) {

        int x = (int) distanceY;
        int b = x / DISTANCE_FACTOR;
        mCurrentBPM += b;
        // Don't go below MINIMUM Tempo
        if (mCurrentBPM < MINIMUM_TEMPO) {
            mCurrentBPM = MINIMUM_TEMPO;
        }
//        Log.d(TAG, "updateTempo ["+b+"] currentTempo ["+mCurrentBPM+"]");
        invalidate();
        fretListener.OnTempoChanged(mCurrentBPM);
    }
}