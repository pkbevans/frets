package com.bondevans.fretboard.fretview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.midi.MidiInputPort;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.bondevans.fretboard.exception.FretboardException;
import com.bondevans.fretboard.midi.MidiFile;
import com.bondevans.fretboard.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FretView displays a fretboard
 */
public class FretboardView extends View {//implements View.OnTouchListener {
    private static final String TAG = "FretboardView";
    private static final double FRET_NUMBER_TEXT_DIVISOR = 1.75;
    private static final int STRING_TEXT_DIVISOR = 12;
    private static final int TEXT_ALPHA = 90;
    private static final int NOTE_TEXT_DIVISOR = 10;
    private static final int SPACE_B4_NUT_DIVISOR = 20;
    private static final int MAX_STRINGS_GUITAR = 6;
    private static final int[] GUITAR_STANDARD_TUNING = new int[]{64, 59, 55, 50, 45, 40};   // Highest to lowest
    private static final String[] GUITAR_STANDARD_TUNING_STRING_NAMES = new String[]{"Top E", "B", "G", "D", "A", "Low E"};
    private static final int MAX_FRETS_GUITAR = 20;
    private static final int MINIMUM_TEMPO = 10;
    private static final int NUT_WIDTH = 5;
    private static final float RANDOM_VALUE = 2;
    private int mFrets = 6;
    private int mStrings = 6;
    //    private int mBottomFret = 5;
    private Paint mPaint;
    private Paint mPaintText;
    private Paint mPaintNote;
    private Paint mPaintBackground;
    private String[] mStringText = {"E", "B", "G", "D", "A", "E"}; // reverse order because we start at the top
    private List<FretNote> mFretNotes;
    private int mFretWidth;
    private int mSpaceBeforeNut;
    private int mStringSpace;
    private List<FretEvent> mFretEvents;
    private int mTicksPerQtrNote;
    private int mDefaultTempo=120;
    private int mCurrentTempo;
    private boolean mPlayEnabled = false;
    private boolean mPlaying = false;
    private GestureDetector gestureDetector;
    private FretEventHandler mFretEventHandler;
    private int mCurrentFretEvent=0;
    private MidiInputPort mInputPort = null;
    int mChannel = 0; // TODO hardcoded channel
    private float mRadius;
    private ProgressListener progressListener;
    private TempoChangeListener tempoChangeListener;
    private TrackLoadedListener trackLoadedListener;
    private Object events;

    public List<FretEvent> getEvents() {
        return mFretEvents;
    }

    public interface ProgressListener{
        void OnProgressUpdated(int progress);
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public interface TempoChangeListener{
        void OnTempoChanged(int tempo);
    }

    public void setTempoChangeListener(TempoChangeListener tempoChangeListener) {
        this.tempoChangeListener = tempoChangeListener;
    }

    public FretboardView(Context context) {
        super(context);
        initialiseView(context);
    }

    /**
     * Constructor
     */
    public FretboardView(Context context, AttributeSet attrs) {
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
//        testDrawNotes();
    }

    /**
     * Initialise the redraw.
     *
     */
    private void initialiseStuff() {
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
        mRadius = mFretWidth/4;
    }

    @Override
    protected void onDraw(Canvas g) {
        initialiseStuff();
        drawStrings(g);
        drawFrets(g);
        drawNotes(g);
        if(mPlayEnabled) {
            drawPlayPauseButton(g);
        }
        doTempo();
    }

    private void doTempo() {
        if(mCurrentTempo==0){
            mCurrentTempo = mDefaultTempo;  // Set it to default instrument first time
        }
    }

    Rect buttonRect;
    Bitmap buttonBitmap;
    private void drawPlayPauseButton(Canvas g) {
        //  Draw the button centred in a square 2xmStringSpace high
        buttonRect = new Rect((getWidth()/2)-mStringSpace,(getHeight()/2)-mStringSpace,(getWidth()/2)+mStringSpace,(getHeight()/2)+mStringSpace);
        if (mPlaying) {
            // Draw Pause button
            buttonBitmap = BitmapFactory.decodeResource(
                    getResources(),
                    R.drawable.pause_button);
        } else {
            // Draw Play button
            buttonBitmap = BitmapFactory.decodeResource(
                    getResources(),
                    R.drawable.play_button);
        }
        g.drawBitmap(buttonBitmap, null, buttonRect, null);
    }

    Rect mRect = new Rect(); // Avoid creating every call to drawStrings
    /**
     * Draw the strings and root note names on the fretboard
     * @param g Canvas to draw on
     */
    private void drawStrings(Canvas g) {
        int string = 1, stringY = 0, textHeight=0;
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

//        Log.d(TAG, "textSize="+mPaintText.getTextSize());
        while (fret <= mFrets) {
            //Draw vertical line - from top to bottom string
            fretX = mSpaceBeforeNut + (fret * mFretWidth);
            if(fret == 0){
                // If fret=0 - i.e. this is the nut then do a dboule line
                g.drawRect(fretX-NUT_WIDTH, mStringSpace, fretX, getHeight() - mStringSpace, mPaint);
            }
            else {
                g.drawLine(fretX, mStringSpace, fretX, getHeight() - mStringSpace, mPaint);
            }
            // Set up mPaint for the Fret number text
            textWidth = (int) mPaintText.measureText("" + (fret + 1));
            // Write fret number above the top string (but not zero)
            if( fret>0) {
                float y = (mStringSpace / 2);
                g.drawText(fret + "", fretX - (mFretWidth / 2) - (textWidth / 2), y, mPaintText);
            }
            ++fret;
        }
    }

    private void drawNotes(Canvas g) {
        if (mFretNotes != null) {
//            Log.d(TAG, "drawNotes ["+mFretNotes.size()+"]");
            for (int i = 0; i < mFretNotes.size(); i++) {
                if (mFretNotes.get(i).on) {
                    // draw a circle on the relevant string in the middle of the fret
//                Log.d(TAG, "note ["+mFretNotes.get(i).note+"] string ["+mFretNotes.get(i).string+
//                        "] fret["+mFretNotes.get(i).fret+"] name ["+mFretNotes.get(i).name+"]");
                    if (mFretNotes.get(i).fret == 0) {
                        // TODO for open strings, draw an un-filled cicle behind the nut
                        mPaintNote.setStyle(Paint.Style.STROKE);
//                        radius = mStringSpace / 8;
                    } else {
//                        radius = mStringSpace / 4;
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
//        Log.d(TAG, "getNoteY ["+ret+"]");
    }

    private float getNoteX(FretNote note) {
        // Get the centre of the given fret
        float ret;
        if (note.fret > 0) {
            ret = (float) (mSpaceBeforeNut + ((note.fret - 1 + 0.5) * mFretWidth));
        } else {
            // If its an open string then we place a Circle just being the nut
            ret = (float) mSpaceBeforeNut - (mRadius*2) - RANDOM_VALUE;
        }
//        Log.d(TAG, "getNoteX ["+ret+"]");
        return ret;
    }

    public void setNotes(List<FretNote> notes) {
        mFretNotes = notes;
    }

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
     * Instructs the view to start loading up the specified track in the given midi file
     *
     * @param mf    MidiFile must have been instantiated and header must be loaded
     * @param track Track to load
     */
    public void loadTrack(MidiFile mf, int track) {
        LoadTrackTask loadTrack = new LoadTrackTask(mf, track);// Do this in background
        loadTrack.execute();
    }

    public void loadTrack(MidiFile mf, int track, int tempo, int progress) {
        LoadTrackTask loadTrack = new LoadTrackTask(mf, track, tempo, progress);// Do this in background
        loadTrack.execute();
    }

    /**
     * Moves to new position in the midi event list - in response to user moving the see bar
     * @param progress - percentage through the song
     */
    public void moveTo(int progress) {
        mCurrentFretEvent = (mFretEvents.size()*progress/100)-1;
        if (mCurrentFretEvent<0){
            mCurrentFretEvent = 0;
        }
        Log.d(TAG, "mCurrentFretEvent [" + mCurrentFretEvent+"]");
    }

    public interface TrackLoadedListener{
        void OnTrackLoaded();
        void OnEmptyTrack();
    }

    public void setTrackLoadedListener(TrackLoadedListener trackLoadedListener){
        this.trackLoadedListener  = trackLoadedListener;
    }
    /**
     * Loads up the note events for the selected track.
     */
    class LoadTrackTask extends AsyncTask<Void, Integer, Void> {
        private static final int NOT_SET = -1;
        int track;
        MidiFile mMidiFile;
        int mTempo=NOT_SET;
        int mProgress=NOT_SET;

        public LoadTrackTask(MidiFile mf, int track) {
            this.mMidiFile = mf;
            this.track = track;
        }
        public LoadTrackTask(MidiFile mf, int track, int tempo, int progress) {
            this.mMidiFile = mf;
            this.track = track;
            this.mProgress = progress;
            this.mTempo = tempo;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//            Log.d(TAG, "Loading track onPreExecute");
            // Disable Play
            mPlayEnabled = false;
            // send notes off
            sendMidiNotesOff2();
            if(mFretEvents != null) {
                mFretEvents.clear();
            }
            if(mFretNotes != null) {
                mFretNotes.clear();
            }
            invalidate();   // Force redraw to remove PLay button
            // TODO Show progress bar
        }

        @Override
        protected Void doInBackground(Void... params) {
//            Log.d(TAG, "Loading track: " + track);
            // convert MIDI file into list of fretboard events
            List<MidiNoteEvent> midiNoteEvents;
            try {
                midiNoteEvents = mMidiFile.loadNoteEvents(track);
//                Log.d(TAG, "Got " + midiNoteEvents.size() + " events");
            } catch (FretboardException | IOException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
            //  Loop through list of events.  For each set of events with the same time (i.e. chords)
            //  get the fret positions
//            Log.d(TAG, "Loading Fret Events");
            mFretEvents = new ArrayList<>();
            boolean first = true;
            FretPosition fp = new FretPosition(MAX_STRINGS_GUITAR, MAX_FRETS_GUITAR,
                    GUITAR_STANDARD_TUNING, GUITAR_STANDARD_TUNING_STRING_NAMES);
            List<FretNote> fretNotes = new ArrayList<>();
            int count = 0;
            int deltaTime = 0;
            int tempo=0;
            for (MidiNoteEvent ev : midiNoteEvents) {
                if (!first && ev.deltaTime > 0) {
                    // If we get an event with a delay then get fret positions for the previous set,
                    // reset count to zero and start building the next set.
//                    Log.d(TAG, "Getting positions for [" + count + "] notes");
                    fretNotes = fp.getFretPositions(fretNotes);
                    mFretEvents.add(new FretEvent(deltaTime, fretNotes, tempo));
                    // calculate delay time and save for later
                    deltaTime = ev.deltaTime;
                    //reset the list of notes
                    fretNotes = new ArrayList<>();
                    count = 0;
                    tempo=0;
                }
//                Log.d(TAG, "Got event [" + ev.on + "][" + ev.name + "][" + ev.note + "]["+ev.instrument+"]");
                first = false;
                if(ev.type == MidiNoteEvent.TYPE_NOTE) {
                    fretNotes.add(new FretNote(ev.note, ev.on));
                }
                else{
                    tempo = ev.tempo;
                }
                count++;
            }
            // Don't forget the last one - and dont add one if there weren't any events (first=true)
            if (!first) {
//                Log.d(TAG, "Getting positions for [" + count + "] notes (Last one)");
                mFretEvents.add(new FretEvent(deltaTime, fp.getFretPositions(fretNotes), 0));
            }
//            Log.d(TAG, "Got [" + mFretEvents.size() + "] FretEvents");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
//            Log.d(TAG, "Loading track onPostExecute");
            super.onPostExecute(aVoid);
            if( mTempo != NOT_SET ){
                mCurrentTempo = mTempo;
            }

            if(mProgress == NOT_SET) {
                mCurrentFretEvent = 0;
            }
            else{
                moveTo(mProgress);
            }
            mTicksPerQtrNote = mMidiFile.getTicksPerQtrNote();
            if (mFretEvents.size() > 0) {
                // Enable Play
                sendMidiProgramChange();
                mPlayEnabled = true;
                invalidate();   // Force redraw to display Play button
                if( trackLoadedListener != null) {
                    trackLoadedListener.OnTrackLoaded();
                }
                if(tempoChangeListener!= null) {
                    tempoChangeListener.OnTempoChanged(mCurrentTempo);
                }
            } else {
                Log.d(TAG, "onPostExecute - No notes in this track");
                // Notify user that there are no notes
                if( trackLoadedListener != null){
                    trackLoadedListener.OnEmptyTrack();
                }
            }
        }
    }

    private byte[] midiBuffer = new byte[3];
    private void sendMidiNote(FretNote fretNote) {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
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
            } else {
                Log.e(TAG, "OOPs - Midi Port NULL 4");
            }
        }
    }

    private void sendMidiNotesOff2(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
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
            } else {
                Log.e(TAG, "OOPs - Midi Port NULL 3");
            }
        }
    }
    private void sendMidiNotesOff() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
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
            } else {
                Log.e(TAG, "OOPs - Midi Port NULL 1");
            }
        }
    }

    private void sendMidiProgramChange(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
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
            } else {
                Log.e(TAG, "OOPs - Midi Port NULL 2");
            }
        }
    }
    class FretEventHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
//            Log.d(TAG, "handleMessage fretEvent[" + mCurrentFretEvent + "]");
            if (mPlaying) {
                // Handle event
                handleEvent(mFretEvents.get(mCurrentFretEvent));
            }
        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }

    private void handleEvent(FretEvent fretEvent){
        // Send next set of notes to Fretboard View
        if(fretEvent.tempo>0){
            mDefaultTempo = fretEvent.tempo;
        }
        setNotes(fretEvent.fretNotes);
        // Force redraw
        invalidate();
        if (++mCurrentFretEvent >= mFretEvents.size()) {
            Log.d(TAG, "No more fretEvents - resetting to zero");
            mCurrentFretEvent = 0;
        }
        long delay = delayFromClicks(mFretEvents.get(mCurrentFretEvent).deltaTime);
//        Log.d(TAG, "setting up fretEvent :" + mCurrentFretEvent + " delay: " + delay);

        // Update progress listener (so it can update the seekbar (or whatever)
        if(progressListener != null){
            progressListener.OnProgressUpdated(mCurrentFretEvent*100/mFretEvents.size());
        }
        mFretEventHandler.sleep(delay);
    }

    /**
     * Plays/pauses the current track
     */
    private void play() {
        // toggle play/pause
        mPlaying = !mPlaying;
        if (mPlaying) {
            // Play
//            Log.d(TAG, "PLAY");
            mFretEventHandler.sleep(mFretEvents.get(mCurrentFretEvent).deltaTime);
        } else {
            // Pause
//            Log.d(TAG, "PAUSE");
            sendMidiNotesOff2();
        }
        invalidate();   // Force redraw with correct play/pause button
    }

    /**
     * Public method to pause if the app is being paused.
     */
    public void pause(){
        if(mPlayEnabled && mPlaying){
            play();
        }
    }
    /**
     * Calculates the time between FretEvents from the delta time (clicks) and the mTicks per beat setting
     * in the file
     *
     * @param deltaTicks    The delta time in ticks for this event
     * @return Returns the actual delay in millisecs for this event
     */
    private long delayFromClicks(int deltaTicks) {
        long ret=0;

        if(mTicksPerQtrNote >0 && deltaTicks>0){
            // Avoid divide by zero error
            double x = ((60*1000)/mCurrentTempo);
            double y = x/mTicksPerQtrNote;
            double z = deltaTicks * y;
            ret = (long) z;
        }

//        Log.d(TAG, "ticksPerQtrNote["+ mTicksPerQtrNote + "] deltaTicks[" + deltaTicks+ "] millisecs["+ret+"]");
        return ret;
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "HELLO onFling");
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
//            Log.d(TAG, "HELLO onSingleTapConfirmed");
            if (mPlayEnabled) {
                // Toggle Play/Pause
                play();
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "HELLO onScroll distanceX="+distanceX+"] distanceY=["+distanceY+"]");
            if( Math.abs(distanceX)> Math.abs(distanceY)){
                Log.d(TAG, "MORE LEFT/RIGHT than UP/DOWN - IGNORE");
            }
            else{
                Log.d(TAG, "MORE UP/DOWN THAN LEFT/RIGHT");
                if(distanceY>0) {
                    Log.d(TAG, "UP = SLOW DOWN");
                }
                else{
                    Log.d(TAG, "DOWN = SPEED UP");
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

        int x = (int)distanceY;
        int b = x/DISTANCE_FACTOR;
        mCurrentTempo += b;
        // Don't go below MINIMUM Tempo
        if(mCurrentTempo< MINIMUM_TEMPO){
            mCurrentTempo = MINIMUM_TEMPO;
        }
//        Log.d(TAG, "updateTempo ["+b+"] currentTempo ["+mCurrentTempo+"]");
        invalidate();
        if(tempoChangeListener != null){
            tempoChangeListener.OnTempoChanged(mCurrentTempo);
        }
    }

    /**
     * Increment current fret by +ve or -ve value
     * @param value value to add or subtract from current fret event
     */
    @Deprecated
    public void incFretEvent(int value) {
        mCurrentFretEvent+=value;
        if(mCurrentFretEvent > mFretEvents.size() ){
            mCurrentFretEvent=mFretEvents.size()-1;
        }
        else if(mCurrentFretEvent < 0 ){
            mCurrentFretEvent = 0;
        }
        Log.d(TAG, "mCurrentFretEvent ["+mCurrentFretEvent+"]");
    }
}