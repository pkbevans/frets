package com.bondevans.frets.fretview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.bondevans.frets.R;
import com.bondevans.frets.instruments.FretInstrument;

import java.util.ArrayList;
import java.util.List;

/**
 * FretView displays a fretboard and a set of notes
 */
public class FretView extends View {
    private static final String TAG = FretView.class.getSimpleName();
    private static final double FRET_NUMBER_TEXT_DIVISOR = 1.75;
    public static final int ZERO_PITCH_BEND = 8192;
    public static final int MAX_BEND = 10;
    private static final int STRING_TEXT_DIVISOR = 12;
    private static final int TEXT_ALPHA = 90;
    private static final int NOTE_TEXT_DIVISOR = 10;
    private static final int NUT_WIDTH = 5;
    private static final float RANDOM_VALUE = 4;
    public static final int BAD_FRET = -1;
    private int mFrets = 22;
    private int mStrings = 6;
    private boolean[] mBentStrings;
    private Paint mPaint;
    private Paint mPaintText;
    private Paint mPaintNote;
    private Paint mPaintBackground;
    private String[] mStringText = {"E", "B", "G", "D", "A", "E"}; // reverse order because we start at the top
    private int mFretWidth;
    private int mSpaceBeforeNut;
    private int mStringSpace;
    protected List<FretNote> mFretNotes;
    private List<FretNote>[] mOldNotes;
    protected float mRadius;
    private boolean mInitialised = false;
    private Bitmap mFretMarkerBM;

    public FretView(Context context) {
        super(context);
        Log.d(TAG, "HELLO constructor 1");
        initialiseView();
    }

    /**
     * Constructor
     */
    public FretView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "HELLO constructor2");
        initialiseView();
    }

    public void setFretInstrument(FretInstrument.Instrument instrument ){
        Log.d(TAG, "HELLO setFretInstrument");
        mStrings = instrument.numStrings();
        mFrets = instrument.numFrets();
        mStringText = instrument.STRING_NAMES();
        mInitialised=false;
        initialiseView();
    }
    /**
     * All initialisation stuff
     */
    protected void initialiseView() {
        Log.d(TAG, "HELLO initialiseView");
        // FretTrackView should call super.initialiseView()
        setBackgroundResource(R.drawable.wood2);
        mFretMarkerBM = BitmapFactory.decodeResource(getResources(), R.drawable.fret_marker);
        mOldNotes = (List<FretNote>[]) new List[5];
        mBentStrings = new boolean[mStrings];
    }

    /**
     * Initialise the redraw.
     */
    private void initialiseStuff() {
        Log.d(TAG, "HELLO initialiseStuff 1");
        if (!mInitialised) {
            Log.d(TAG, "HELLO initialiseStuff 2");
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
        Log.d(TAG, "HELLO onDraw");
        initialiseStuff();
        drawBackground(g);
        drawFrets(g);
        drawOldNotes(g);
        drawNotes(g);
        drawStrings(g);
    }

    Rect mRect = new Rect(); // Avoid creating every call to drawStrings

    void drawBackground(Canvas g) {
        // Blank out area above top string and below bottom
        g.drawRect(0, 0, getWidth(), mStringSpace - 1, mPaintBackground);
        // Blank out area below bottom string
        g.drawRect(0, (mStrings * mStringSpace) + 1, getWidth(), getHeight(), mPaintBackground);
    }

    /**
     * Draw the strings and root note names on the fretboard
     *
     * @param g Canvas to draw on
     */
    private void drawStrings(Canvas g) {
        int stringY, textHeight;
        // Set up mPaintText for String text
        mPaintText.setTextSize(getHeight() / STRING_TEXT_DIVISOR);
        mPaintText.setColor(Color.BLUE);
        mPaintText.setAlpha(TEXT_ALPHA);

        // Draw  Strings
        int string = 0;
        while (string < mStrings) {
            stringY = (string + 1) * mStringSpace;
            if (!mBentStrings[string]) {
                // Draw string
                g.drawLine(mSpaceBeforeNut, stringY, getWidth(), stringY, mPaint);
            }
            // Write String note
            mPaintText.getTextBounds(mStringText[string], 0, 1, mRect);
            textHeight = mRect.height();
            g.drawText(mStringText[string], 0, stringY + textHeight / 2, mPaintText);
            ++string;
        }
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
            mPaintText.setTextSize((float) (mFretWidth / FRET_NUMBER_TEXT_DIVISOR));
        mPaintText.setColor(Color.GREEN);
        mPaintText.setAlpha(TEXT_ALPHA);

        while (fret <= mFrets) {
            //Draw vertical line - from top to bottom string
            fretX = mSpaceBeforeNut + (fret * mFretWidth);
            if (fret == 0) {
                // If fret=0 - i.e. this is the nut then do a double line
                g.drawRect(fretX - NUT_WIDTH, mStringSpace, fretX, getHeight() - mStringSpace, mPaint);
            } else {
                g.drawLine(fretX, mStringSpace, fretX, getHeight() - mStringSpace, mPaint);
            }
            // Set up mPaint for the Fret number text
            textWidth = (int) mPaintText.measureText("" + (fret + 1));
            // Write fret number (but not zero)
            if (fret > 0) {
                float y = mStringSpace/2;
                g.drawText(fret + "", fretX - (mFretWidth / 2) - (textWidth / 2), y, mPaintText);
            }
            // Add fret markers at frets 3, 5, 7, 9, 12, 15, 17, 19
            if (fret == 2 || fret == 4 || fret == 6 || fret == 8 || fret == 14 || fret == 16 || fret == 18) {
                g.drawBitmap(mFretMarkerBM, null, getFretMarkerRect(fretX, getHeight()/2), null);
            } else if (fret == 11) {
                // two on the 12th fret
                g.drawBitmap(mFretMarkerBM, null, getFretMarkerRect(fretX, getHeight()/3), null);
                g.drawBitmap(mFretMarkerBM, null, getFretMarkerRect(fretX, (getHeight()/3)*2), null);
            }
            ++fret;
        }
    }

    private Rect getFretMarkerRect(int fretX, int yMiddle) {
        // We want a Rect in between current fret and next, centred on the fret board
        int left, top, right, bottom;
        left = fretX + (mFretWidth / 4);
        top = yMiddle - (mFretWidth / 4);
        right = left + (mFretWidth / 2);
        bottom = top + (mFretWidth / 2);
        return new Rect(left, top, right, bottom);
    }

    private Rect getFretMarkerRectOld(int fretX, int string) {
        // We want a Rect in between current fret and next, centred on the fret board
        int left, top, right, bottom;
        left = fretX + (mFretWidth / 4);
        top = (mStringSpace * string) + (mStringSpace / 2) - (mFretWidth / 4);
        right = left + (mFretWidth / 2);
        bottom = top + (mFretWidth / 2);
        return new Rect(left, top, right, bottom);
    }

    /**
     * Draw current Notes
     * Default method does not play midi note. FretTrackView should overwrite but call super
     *
     * @param g Canvas
     */
    private void drawNotes(Canvas g) {
        for (int i = 0; i < mBentStrings.length; i++) {
            mBentStrings[i] = false;
        }
        if (mFretNotes != null) {
            for (int i = 0; i < mFretNotes.size(); i++) {
                if (mFretNotes.get(i).on) {
                    // draw a circle on the relevant string in the middle of the fret
                    mPaintNote.setStyle(Paint.Style.FILL);
                    mPaintNote.setColor(Color.BLACK);
                    mPaintNote.setAlpha(255);
                    float y = getNoteY(mFretNotes.get(i));
                    float x = getNoteX(mFretNotes.get(i));
                    g.drawCircle(x, y, mRadius, mPaintNote);
                    if (mFretNotes.get(i).bend > 0) {
                        mBentStrings[mFretNotes.get(i).string] = true;
                        // Draw bent string
                        int y2 = (mFretNotes.get(i).string + 1) * mStringSpace;
                        // Line from nut to note
                        g.drawLine(mSpaceBeforeNut, y2, x, y, mPaint);
                        // line from not to bridge
                        g.drawLine(x, y, getWidth(), y2, mPaint);
                    }
                }
            }
        }
    }

    private void drawOldNotes(Canvas g) {
        for (int n = 0; n < mOldNotes.length; n++) {
            for (int i = 0; mOldNotes[n] != null && (i < mOldNotes[n].size()); i++) {
                if (mOldNotes[n].get(i).on) {
                    // draw a circle on the relevant string in the middle of the fret
                    mPaintNote.setStyle(Paint.Style.FILL);
                    mPaintNote.setColor(Color.RED);
                    mPaintNote.setAlpha(150 - (n * 30));  // Gets progressively feinter
                    g.drawCircle(getNoteX(mOldNotes[n].get(i)), getNoteY(mOldNotes[n].get(i)), mRadius, mPaintNote);
                }
            }
        }
    }
    private float getNoteY(FretNote note) {
        // Get the y coord of the given string - allowing for bent strings
        // Turn the midi pitchwheel value into a string-bend value between 0-10
        int bend = note.bend > ZERO_PITCH_BEND ? (note.bend - ZERO_PITCH_BEND) / (ZERO_PITCH_BEND / MAX_BEND) : 0;
        return ((note.string + 1) * mStringSpace) + (bend * mStringSpace / FretEvent.MAX_BEND);
    }

    private float getNoteX(FretNote note) {
        // Get the centre of the given fret
        float ret;
        if (note.fret > 0) {
            ret = (float) (mSpaceBeforeNut + ((note.fret - 1 + 0.5) * mFretWidth));
        } else {
            // If its an open string then we place a Circle just being the nut
            ret = (float) mSpaceBeforeNut - mRadius - RANDOM_VALUE;
        }
        return ret;
    }

    public void setNotes(FretEvent fretEvent) {
        // Could be just a bend on current notes, so don't overwrite if notes list empty
        if (fretEvent.fretNotes.size() > 0) {
            // Store the previous notes so we can draw a ghostly trail of previous notes
            System.arraycopy(mOldNotes, 0, mOldNotes, 1, mOldNotes.length - 2 + 1);
            mOldNotes[0] = mFretNotes;
            mFretNotes = fretEvent.fretNotes;
        }
        // Apply bend to current notes
        if (mFretNotes != null) {
            for (FretNote fn : mFretNotes) {
                if (fn.on) {
                    fn.bend = fretEvent.bend;
                }
            }
        }
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
        FretEvent fretEvent = new FretEvent(0,myFretNotes,120,0,0);

        setNotes(fretEvent);
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

    /**
     * Get the fret for a given Y Coordinate
     * @param xCoord X Coordinate
     * @return the fret that the given X coordinate falls on
     */
    protected int getFret(float xCoord){
        for (int fret=mFrets;fret >=0 ;fret--) {
            //Draw vertical line - from top to bottom string
            int fretX = mSpaceBeforeNut + (fret * mFretWidth);
            if(fretX<xCoord){
                if(fret==mFrets){
                    // Higher than the last fret
                    return BAD_FRET;
                }
                return fret+1;
            }
        }
        // Lower than the first fret
        return BAD_FRET;
    }
}