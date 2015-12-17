package com.bondevans.fretboard.fretview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.midi.MidiInputPort;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.bondevans.fretboard.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FretView displays a fretboard
 */
public class FretView extends View {
    private static final String TAG = FretView.class.getSimpleName();
    private static final double FRET_NUMBER_TEXT_DIVISOR = 1.75;
    private static final int STRING_TEXT_DIVISOR = 12;
    private static final int TEXT_ALPHA = 90;
    private static final int NOTE_TEXT_DIVISOR = 10;
    private static final int MINIMUM_TEMPO = 10;
    private static final int NUT_WIDTH = 5;
    private static final float RANDOM_VALUE = 2;
    private int mFrets = 22;
    private int mStrings = 6;
    private Paint mPaint;
    private Paint mPaintText;
    private Paint mPaintNote;
    private Paint mPaintBackground;
    private String[] mStringText = {"E", "B", "G", "D", "A", "E"}; // reverse order because we start at the top
    private FretTrack mFretTrack;
    private int mFretWidth;
    private int mSpaceBeforeNut;
    private int mStringSpace;
    private List<FretNote> mFretNotes;
    private int mTicksPerQtrNote;
    private int mDefaultTempo = 120;
    private int mCurrentBPM;
    private boolean mPlaying = false;
    private GestureDetector gestureDetector;
    private FretEventHandler mFretEventHandler;
    private int mCurrentFretEvent = 0;
    private MidiInputPort mInputPort = null;
    int mChannel = 0; // TODO hardcoded channel
    private float mRadius;
    private FretListener fretListener;
    private boolean mInitialised = false;

    public interface FretListener {
        void OnProgressUpdated(int numberEvents, int currentEvent);
        void OnTempoChanged(int tempo);

        void OnPlayEnabled(boolean flag);
    }

    public void setFretListener(FretListener fretListener) {
        this.fretListener = fretListener;
    }

    public FretView(Context context) {
        super(context);
        initialiseView(context);
    }

    /**
     * Constructor
     */
    public FretView(Context context, AttributeSet attrs) {
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
        gestureDetector = new GestureDetector(context, new MyGestureListener());
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };

        this.setOnTouchListener(gestureListener);
        mFretEventHandler = new FretEventHandler();
        setBackgroundResource(R.drawable.wood2);
    }

    /**
     * Initialise the redraw.
     */
    private void initialiseStuff() {
        if (!mInitialised) {
            // Set up a Paint for the strings and frets
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(Color.BLACK);
            mPaint.getFontSpacing();
            // Set up a Paint for the Fret Number and String text
            mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaintText.setTextAlign(Paint.Align.LEFT);
            // Set up a Paint for the FretNote dots
            mPaintNote = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaintNote.setTextSize(getHeight() / NOTE_TEXT_DIVISOR);
            // Set up a Paint for background
            mPaintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaintBackground.setColor(Color.WHITE);

            // various constants...
            mStringSpace = getHeight() / (mStrings + 1);
            mSpaceBeforeNut = getWidth() / mFrets;
            mFretWidth = (getWidth() - (mSpaceBeforeNut * 2)) / mFrets;
            mRadius = mFretWidth / 4;
            mInitialised = true;
        }
    }

    @Override
    protected void onDraw(Canvas g) {
        initialiseStuff();
        drawStrings(g);
        drawFrets(g);
        drawNotes(g);
        doTempo();
    }

    private void doTempo() {
        if (mCurrentBPM == 0) {
            mCurrentBPM = mDefaultTempo;  // Set it to default instrument first time
        }
    }

    Rect mRect = new Rect(); // Avoid creating every call to drawStrings

    /**
     * Draw the strings and root note names on the fretboard
     *
     * @param g Canvas to draw on
     */
    private void drawStrings(Canvas g) {
        int string = 1, stringY = 0, textHeight = 0;
        // Set up mPaintText for String text
        mPaintText.setTextSize(getHeight() / STRING_TEXT_DIVISOR);
        mPaintText.setColor(Color.BLUE);
        mPaintText.setAlpha(TEXT_ALPHA);

        // Blank out area above top string and below bottom
        g.drawRect(0, 0, getWidth(), mStringSpace - 1, mPaintBackground);
        while (string <= mStrings) {
            // Draw string
            stringY = string * mStringSpace;
            g.drawLine(0, stringY, getWidth(), stringY, mPaint);
            // Write String note
            mPaintText.getTextBounds(mStringText[string - 1], 0, 1, mRect);
            textHeight = mRect.height();
            g.drawText(mStringText[string - 1], 0, stringY + textHeight / 2, mPaintText);
            ++string;
        }
        // Blank out area below bottom string
        g.drawRect(0, stringY + 1, getWidth(), getHeight(), mPaintBackground);
        // Redraw text...
        g.drawText(mStringText[mStrings - 1], 0, stringY + textHeight / 2, mPaintText);
    }

    /**
     * Draw the frets on the fretboard
     *
     * @param g Canvas to draw on
     */
    private void drawFrets(Canvas g) {
        //get width and divide by number of frets
        int fret = 0;
        // Allow a bit of space before and after the first fret (the nut) and the last one
        int fretX;
        int textWidth;
        mPaintText.setTextSize((float) (mStringSpace / FRET_NUMBER_TEXT_DIVISOR));
        mPaintText.setColor(Color.GREEN);
        mPaintText.setAlpha(TEXT_ALPHA);

        while (fret <= mFrets) {
            //Draw vertical line - from top to bottom string
            fretX = mSpaceBeforeNut + (fret * mFretWidth);
            if (fret == 0) {
                // If fret=0 - i.e. this is the nut then do a dboule line
                g.drawRect(fretX - NUT_WIDTH, mStringSpace, fretX, getHeight() - mStringSpace, mPaint);
            } else {
                g.drawLine(fretX, mStringSpace, fretX, getHeight() - mStringSpace, mPaint);
            }
            // Set up mPaint for the Fret number text
            textWidth = (int) mPaintText.measureText("" + (fret + 1));
            // Write fret number above the top string (but not zero)
            if (fret > 0) {
                float y = (mStringSpace / 2);
                g.drawText(fret + "", fretX - (mFretWidth / 2) - (textWidth / 2), y, mPaintText);
            }
            ++fret;
        }
    }

    private void drawNotes(Canvas g) {
        if (mFretNotes != null) {
            for (int i = 0; i < mFretNotes.size(); i++) {
                if (mFretNotes.get(i).on) {
                    // draw a circle on the relevant string in the middle of the fret
                    if (mFretNotes.get(i).fret == 0) {
                        // For open strings, draw an un-filled cicle behind the nut
                        mPaintNote.setStyle(Paint.Style.STROKE);
                    } else {
                        mPaintNote.setStyle(Paint.Style.FILL);
                    }
                    g.drawCircle(getNoteX(mFretNotes.get(i)), getNoteY(mFretNotes.get(i)), mRadius, mPaintNote);
                }
                // Send the MIDI note to the Input Port
                sendMidiNote(mFretNotes.get(i));
            }
        }
    }

    private float getNoteY(FretNote note) {
        // Get the y coord of the given string
        return (note.string + 1) * mStringSpace;
    }

    private float getNoteX(FretNote note) {
        // Get the centre of the given fret
        float ret;
        if (note.fret > 0) {
            ret = (float) (mSpaceBeforeNut + ((note.fret - 1 + 0.5) * mFretWidth));
        } else {
            // If its an open string then we place a Circle just being the nut
            ret = (float) mSpaceBeforeNut - (mRadius * 2) - RANDOM_VALUE;
        }
        return ret;
    }

    private void setNotes(List<FretNote> notes) {
        mFretNotes = notes;
    }

    @SuppressWarnings("unused")
    void testDrawNotes() {
        List<FretNote> myFretNotes = new ArrayList<>();
        myFretNotes.add(new FretNote(40, true, 5, 0, "E"));
        myFretNotes.add(new FretNote(46, true, 4, 1, "Bb"));
        myFretNotes.add(new FretNote(52, true, 3, 2, "E"));
        myFretNotes.add(new FretNote(58, true, 2, 3, "Bb"));
        myFretNotes.add(new FretNote(63, true, 1, 4, "Eb"));
        myFretNotes.add(new FretNote(69, true, 0, 5, "A"));

        setNotes(myFretNotes);
    }

    /**
     * Instructs the view to start loading up the specified track in the FretSong
     *
     * @param fretTrack Track to load
     * @param tpqn ticks per quarter note
     * @param bpm BPM
     */
    public void setTrack(FretTrack fretTrack, int tpqn, int bpm) {
        setTrack(fretTrack, tpqn, bpm, 0);
    }

    public void setTrack(FretTrack frettrack, int tpqn, int bpm, int currentFretEvent) {
        Log.d(TAG, "setTrack");
        mFretTrack = frettrack;
        mCurrentBPM = bpm;
        mCurrentFretEvent = currentFretEvent;
        setNotes(mFretTrack.fretEvents.get(mCurrentFretEvent).fretNotes);
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

    private void sendMidiNotesOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mInputPort != null) {
//            Log.d(TAG, "sendMidiNotesOff");
                for (int channel = 0; channel < 16; channel++) {
                    for (int i = 0; i < 128; i++) {
                        midiBuffer[0] = (byte) (0x80 | channel);
                        // Note value
                        midiBuffer[1] = (byte) i;
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
        setNotes(fretEvent.fretNotes);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Log.d(TAG, "onMeasure getWidth ["+getWidth()+"]getHeight["+getHeight()+"]widthMeasureSpec["
//                +widthMeasureSpec+"] heightMeasureSpec["+heightMeasureSpec+"]");
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = height * 3;
//        Log.d(TAG, "onMeasure getWidth [" + getWidth() + "]getHeight[" + getHeight() + "]width["
//                + width + "] height[" + height + "]");

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }
}