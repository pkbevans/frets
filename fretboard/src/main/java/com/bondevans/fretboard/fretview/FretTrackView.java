package com.bondevans.fretboard.fretview;

import android.content.Context;
import android.graphics.Canvas;
import android.media.midi.MidiInputPort;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;

/**
 * <code>FretTrackView</code> extends <code>FretView</code>  to animate the notes of a song on a fretboard
 */
public class FretTrackView extends FretView {
    private static final String TAG = FretTrackView.class.getSimpleName();
    private static final int MINIMUM_TEMPO = 10;
    private FretTrack mFretTrack;
    private int mTicksPerQtrNote;
    private int mDefaultTempo = 120;
    private int mCurrentBPM;
    private boolean mPlaying = false;
    private GestureDetector gestureDetector;
    private FretEventHandler mFretEventHandler;
    private int mCurrentFretEvent = 0;
    private MidiInputPort mInputPort = null;
    int mChannel = 0; // TODO hardcoded channel
    private FretListener fretListener;

    public interface FretListener {
        void OnProgressUpdated(int numberEvents, int currentEvent);

        void OnTempoChanged(int tempo);

        void OnPlayEnabled(boolean flag);
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

    public void setInputPort(MidiInputPort inputPort, int channel) {
        this.mInputPort = inputPort;
        this.mChannel = channel;
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
        doMidiNotes();
    }

    private void doTempo() {
        if (mCurrentBPM == 0) {
            mCurrentBPM = mDefaultTempo;  // Set it to default instrument first time
        }
    }

    private void doMidiNotes() {
        if (mFretNotes != null) {
            for (int i = 0; i < mFretNotes.size(); i++) {
                // Send the MIDI note to the Input Port
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
        mCurrentBPM = bpm;
        mCurrentFretEvent = currentFretEvent;
        setNotes(mFretTrack.fretEvents.get(mCurrentFretEvent).fretNotes, mFretTrack.fretEvents.get(mCurrentFretEvent).bend);
        mTicksPerQtrNote = tpqn;
        // Enable Play
        sendMidiProgramChange();
        fretListener.OnPlayEnabled(true);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mInputPort != null) {
//            Log.d(TAG, (fretNote.on?"NOTE ON": "NOTE OFF")+ " ["+fretNote.name+"]");
                midiBuffer[0] = (byte) (fretNote.on ? (0x90 | mChannel) : (0x80 | mChannel));
                // Note value
                midiBuffer[1] = (byte) fretNote.note;
                // Velocity - TODO Hardcoded volume
                midiBuffer[2] = (byte) 0x60;
                try {
                    mInputPort.send(midiBuffer, 0, 3);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "OOPS Midi send didn't work");
                }
            }
        }
    }

    private void sendMidiNotesOff2() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mInputPort != null) {
                Log.d(TAG, "Sending ALL NOTES OFF");
                midiBuffer[0] = (byte) (0xB | mChannel);
                // Note value
                midiBuffer[1] = (byte) 0x7B;
                // Velocity - TODO Hardcoded volume
                try {
                    mInputPort.send(midiBuffer, 0, 2);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "OOPS Midi sendNotesOffdidn't work");
                }
            }
        }
    }

    private void sendMidiProgramChange() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mInputPort != null) {
                Log.d(TAG, "sendMidiProgramChange");
                midiBuffer[0] = (byte) (0xC | mChannel);
                // Note value
                midiBuffer[1] = 0x1D;
                // Velocity - TODO Hardcoded volume
                midiBuffer[2] = (byte) 0;
                try {
                    mInputPort.send(midiBuffer, 0, 3);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "OOPS Midi send didn't work");
                }
            }
        }
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
            sendMidiNotesOff2();
        }
        invalidate();   // Force redraw with correct play/pause button
    }

    /**
     * Public method to pause if the app is being paused.
     */
    public void pause() {
        if (mPlaying) {
            play();
        }
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