package com.bondevans.fretboard.fretview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.bondevans.fretboard.R;

import java.util.ArrayList;
import java.util.List;

/**
 * FretView displays a fretboard
 */
public class FretView extends View {
    //    private static final String TAG = FretView.class.getSimpleName();
    private static final double FRET_NUMBER_TEXT_DIVISOR = 1.75;
    private static final int STRING_TEXT_DIVISOR = 12;
    private static final int TEXT_ALPHA = 90;
    private static final int NOTE_TEXT_DIVISOR = 10;
    private static final int NUT_WIDTH = 5;
    private static final float RANDOM_VALUE = 2;
    private int mFrets = 22;
    private int mStrings = 6;
    private Paint mPaint;
    private Paint mPaintText;
    private Paint mPaintNote;
    private Paint mPaintBackground;
    private String[] mStringText = {"E", "B", "G", "D", "A", "E"}; // reverse order because we start at the top
    private int mFretWidth;
    private int mSpaceBeforeNut;
    private int mStringSpace;
    protected List<FretNote> mFretNotes;
    private float mRadius;
    private boolean mInitialised = false;

    public FretView(Context context) {
        super(context);
        initialiseView();
    }

    /**
     * Constructor
     */
    public FretView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseView();
    }

    /**
     * All initialisation stuff
     */
    protected void initialiseView() {
        // FretTrackView should call super.initialiseView()
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

    /**
     * Draw current Notes
     * Default method does not play midi note. FretTrackView should overwrite but call super
     *
     * @param g Canvas
     */
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

    public void setNotes(List<FretNote> notes) {
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