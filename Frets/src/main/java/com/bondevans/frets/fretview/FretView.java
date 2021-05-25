package com.bondevans.frets.fretview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.bondevans.frets.R;
import com.bondevans.frets.instruments.FretInstrument;
import com.bondevans.frets.utils.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.atan2;

/**
 * FretView displays a fretboard and a set of notes
 */
public class FretView extends View {
    private static final String TAG = FretView.class.getSimpleName();
    public static final int ZERO_PITCH_BEND = 8192;
    public static final int MAX_BEND = 10;
    private static final int STRING_TEXT_DIVISOR = 14;
    private static final int TEXT_ALPHA = 190;
    private static final int NOTE_TEXT_DIVISOR = 10;
    private static final float RANDOM_VALUE = 4;
    public static final int BAD_FRET = -1;
    private int mFrets = 20;
    private int mStrings = 6;
    private String[] mStringText = {"E", "B", "G", "D", "A", "E"}; // reverse order because we start at the top
    private boolean[] mBentStrings;
    private Paint mPaintText;
    private Paint mPaintNote;
    private Paint mPaintOldNote;
    private int mFretWidth;
    private int mSpaceBeforeNut;
    private int mFretboardWhiteSpace;
    protected List<FretNote> mFretNotes;
    private List<FretNote>[] mOldNotes;
    protected float mRadius;
    private boolean mInitialised = false;
    private Bitmap [] mStringBM = new Bitmap[6];
    private final boolean l2R = true;
    private int mStringSpace;
    private Rect mRect = new Rect(); // Avoid creating every call to drawStrings
    private Matrix mMatrix = new Matrix();

    public FretView(Context context) {
        super(context);
//        Log.d(TAG, "HELLO constructor 1");
        initialiseView();
    }

    /**
     * Constructor
     */
    public FretView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        Log.d(TAG, "HELLO constructor2");
        initialiseView();
    }

    public void setFretInstrument(FretInstrument.Instrument instrument ){
//        Log.d(TAG, "HELLO setFretInstrument");
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
//        Log.d(TAG, "HELLO initialiseView");
        // FretTrackView should call super.initialiseView()
        setBackgroundResource(R.drawable.fretboard_20_frets_large);
        mStringBM[0] = BitmapFactory.decodeResource(getResources(), R.drawable.string_1_e);
        mStringBM[1] = BitmapFactory.decodeResource(getResources(), R.drawable.string_2_b);
        mStringBM[2] = BitmapFactory.decodeResource(getResources(), R.drawable.string_3_g);
        mStringBM[3] = BitmapFactory.decodeResource(getResources(), R.drawable.string_4_d);
        mStringBM[4] = BitmapFactory.decodeResource(getResources(), R.drawable.string_5_a);
        mStringBM[5] = BitmapFactory.decodeResource(getResources(), R.drawable.string_6_e);
        mOldNotes = (List<FretNote>[]) new List[5];
        mBentStrings = new boolean[mStrings];
    }

    /**
     * Initialise the redraw.
     */
    private void initialiseStuff() {
//        Log.d(TAG, "HELLO initialiseStuff 1");
        if (!mInitialised) {
//            Log.d(TAG, "HELLO initialiseStuff 2");
            // Set up a Paint for the Fret Number and String text
            mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaintText.setTextAlign(Paint.Align.LEFT);
            // Set up a Paint for the FretNote dots
            mPaintNote = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaintNote.setStyle(Paint.Style.FILL);
            mPaintNote.setColor(Color.BLACK);
            mPaintNote.setAlpha(255);
            mPaintOldNote = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaintOldNote.setStyle(Paint.Style.FILL);
            mPaintOldNote.setColor(Color.RED);
            // various constants...
            mFretboardWhiteSpace = getHeight() / (mStrings + 1);
            int mFretboardHeight = getHeight() - (mFretboardWhiteSpace * 2);
            mStringSpace = mFretboardHeight / mStrings;
            mFretWidth = getWidth() / (mFrets+2);
            mSpaceBeforeNut = mFretWidth;
            mRadius = mFretWidth / 4;
            mInitialised = true;
        }
    }

    @Override
    protected void onDraw(Canvas g) {
        initialiseStuff();
        setBentStrings();
        drawStrings(g);
        drawOldNotes(g);
        drawNotes(g);
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

        // Draw  Strings (highest to lowest)
        int string = 0;
        while (string < mStrings) {
            stringY = getStringY(string);
            if (!mBentStrings[string]) {
                // Draw string
                g.drawBitmap(mStringBM[string], null, getStringRect(string, stringY), null);
            }
            // Write String note
            mPaintText.getTextBounds(mStringText[string], 0, 1, mRect);
            textHeight = mRect.height();
            g.drawText(mStringText[string], 0, stringY + textHeight / 2, mPaintText);
            ++string;
        }
    }
    private Rect getStringRect(int string, int yMiddle) {
        // We want a Rect in between current fret and next, centred on the fret board
        int left, top, right, bottom;

        left=mSpaceBeforeNut;
        top = yMiddle - (mStringBM[string].getHeight() / 2);
        right = getWidth();
        bottom = yMiddle + (mStringBM[string].getHeight() / 2);
        return new Rect(left, top, right, bottom);
    }
    private int getStringY(int string){
        if(l2R){
            return mFretboardWhiteSpace + (mStringSpace/2) + (string * mStringSpace);
        } else {
            return getHeight() - mFretboardWhiteSpace - (mStringSpace/2) - (string * mStringSpace);
        }
    }
    private void setBentStrings() {
        Arrays.fill(mBentStrings, false);
        if (mFretNotes != null) {
            for (int i = 0; i < mFretNotes.size(); i++) {
                if (mFretNotes.get(i).on && mFretNotes.get(i).bend > 0) {
                    mBentStrings[mFretNotes.get(i).string] = true;
                }
            }
        }
    }
    /**
     * Draw current Notes
     * Default method does not play midi note. FretTrackView should overwrite but call super
     *
     * @param g Canvas
     */
    private void drawNotes(Canvas g) {
        float noteY, noteX;
        int stringY;
        double degrees;
        if (mFretNotes != null) {
            for (int i = 0; i < mFretNotes.size(); i++) {
                if (mFretNotes.get(i).on) {
                    // draw a circle on the relevant string in the middle of the fret
                    noteY = getNoteY(mFretNotes.get(i));  // Centre of note
                    noteX = getNoteX(mFretNotes.get(i));  // Centre of note
                    if (mFretNotes.get(i).bend > 0) {
                        // Draw the bent string if the note has a bend on it
                        stringY = getStringY(mFretNotes.get(i).string); // y coord of unbent string
                        // Line from nut to note
                        degrees = Math.toDegrees(atan2((double)(noteY - stringY), (double)(noteX - 0)));
                        mMatrix.setTranslate(noteX, noteY);
                        mMatrix.postRotate((float)degrees+180,noteX, noteY);
                        g.drawBitmap(mStringBM[mFretNotes.get(i).string], mMatrix, null);
                        // line from note to bridge
                        degrees = Math.toDegrees(atan2((double)(stringY - noteY), (double)(getWidth()-noteX)));
                        mMatrix.setRotate((float)degrees,0, stringY);
                        mMatrix.postTranslate(noteX, noteY);
                        g.drawBitmap(mStringBM[mFretNotes.get(i).string], mMatrix, null);
                    }
                    // Draw note
                    g.drawCircle(noteX, (float) noteY, mRadius, mPaintNote);
                }
            }
        }
    }

    private void drawOldNotes(Canvas g) {
        for (int n = 0; n < mOldNotes.length; n++) {
            for (int i = 0; mOldNotes[n] != null && (i < mOldNotes[n].size()); i++) {
                if (mOldNotes[n].get(i).on) {
                    // draw a circle on the relevant string in the middle of the fret
                    mPaintOldNote.setAlpha(150 - (n * 30));  // Gets progressively more feint
                    g.drawCircle(getNoteX(mOldNotes[n].get(i)), getNoteY(mOldNotes[n].get(i)), mRadius, mPaintOldNote);
                }
            }
        }
    }
    private float getNoteY(FretNote note) {
        // Get the y coord of the given string - allowing for bent strings
        // Turn the midi pitchwheel value into a string-bend value between 0-10
        int bend = note.bend > ZERO_PITCH_BEND ? (note.bend - ZERO_PITCH_BEND) / (ZERO_PITCH_BEND / MAX_BEND) : 0;
        return getStringY(note.string) + (bend * mStringSpace / FretEvent.MAX_BEND);
    }
    private float getNoteX(FretNote note) {
        // Get the centre of the given fret
        float ret;
        if (note.fret > 0) {
            if(l2R) {
                ret = (float) (mSpaceBeforeNut + ((note.fret - 1 + 0.5) * mFretWidth));
            } else {
                ret = (float)  (getWidth() - mSpaceBeforeNut - ((note.fret - 1 + 0.5) * mFretWidth));
            }
        } else {
            // If its an open string then we place a Circle just behind the nut
            if(l2R) {
                ret = (float) mSpaceBeforeNut - mRadius - RANDOM_VALUE;
            } else {
                ret = (float) getWidth() - mSpaceBeforeNut + mRadius + RANDOM_VALUE;
            }
        }
        return ret;
    }

    public void setNotes(List <FretNote> fretNotes, int bend) {
        // Could be just a bend on current notes, so don't overwrite if notes list empty
        if (fretNotes.size() > 0) {
            // Store the previous notes so we can draw a ghostly trail of previous notes
            System.arraycopy(mOldNotes, 0, mOldNotes, 1, mOldNotes.length - 2 + 1);
            mOldNotes[0] = mFretNotes;
            mFretNotes = fretNotes;
        }
        // Apply bend to current notes
        if (bend>0 && mFretNotes != null) {
            for (FretNote fn : mFretNotes) {
                if (fn.on) {
                    fn.bend = bend;
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

        setNotes(fretEvent.fretNotes, 0);
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