package com.bondevans.fretboard.fretboardplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul on 27/07/2015.
 */
public class FretboardView extends View {
    static final double FRET_NUMBER_TEXT_DIVISOR=1.75;
    static final int STRING_TEXT_DIVISOR = 12;
    private static final int TEXT_ALPHA = 100;
    private static final int NOTE_TEXT_DIVISOR = 10;
    private static final int SPACE_B4_NUT_DIVISOR = 20;
    private static final String TAG = "FretboardView";
    private int mFrets = 5;
    private int mStrings = 6;
    private int mBottomFret = 5;
    private Paint mPaint;
    private Paint mPaintText;
    private Paint mPaintNote;
    private String[] mStringText={"E","B","G","D","A","E"}; // reverse order because we start at the top
    private List<FretNote> mFretNotes;
    private int mFretWidth;
    private int mSpaceBeforeNut;
    private int mStringSpace;

    public FretboardView(Context context) {
        super(context);
        testDrawNotes();
    }

    /**
     * Constructor
     */
    public FretboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        testDrawNotes();
    }

    void init(Canvas g){
        // Set up a Paint for the strings and frets
        mPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);
        mPaint.getFontSpacing();
        // Set up a Paint for the Fret Number and String text
        mPaintText=new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText.setAlpha(TEXT_ALPHA);       //  Fret Number and string FretNote text is not full power
        mPaintText.setTextAlign(Paint.Align.LEFT);
        // Set up a Paint for the FretNote dots
        mPaintNote=new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintNote.setTextSize(getHeight()/NOTE_TEXT_DIVISOR);
        // Fill the background
        g.drawColor(Color.WHITE);
        // various constants...
        mStringSpace=getHeight()/(mStrings+1);
        mSpaceBeforeNut=getWidth()/SPACE_B4_NUT_DIVISOR;
        mFretWidth=(getWidth()-(mSpaceBeforeNut*2))/mFrets;
    }
    @Override
    protected void onDraw(Canvas g) {
        init(g);
        drawFrets(g);
        drawStrings(g);
        drawNotes(g);
    }

    Rect rect = new Rect(); // Avoid creating every call to drawStrings
    private void drawStrings(Canvas g) {
        Log.d(TAG, "drawStrings");
        int string=1, stringY;
        // Set up mPaintText for String text
        mPaintText.setTextSize(getHeight() / STRING_TEXT_DIVISOR);
        mPaintText.setColor(Color.BLUE);

//        Log.d(TAG, "getHeight=" + getHeight() + " mStringSpace=" + mStringSpace);
        while(string<=mStrings){
            // Draw string
            stringY=string*mStringSpace;
//            Log.d(TAG, "stringY=" + stringY + " getWidth=" + getWidth());
            g.drawLine(0, stringY, getWidth(), stringY, mPaint);
            // Write String note
//            Log.d(TAG, "mStringText="+mStringText[string-1]+" textSize="+mPaintText.getTextSize()+" y="+(stringY+ mPaintText.getTextSize()/2));
            mPaintText.getTextBounds(mStringText[string - 1], 0, 0, rect);
            int textHeight = rect.height();
            g.drawText(mStringText[string-1],0,stringY+ textHeight/2, mPaintText);
//            g.drawText(mStringText[string-1],0,stringY+ mPaintText.getTextSize()/2, mPaintText);
            ++string;
        }
    }

    private void drawFrets(Canvas g) {
        Log.d(TAG, "drawFrets");
        //get width and divide by number of frets
        int fret = 0;
        // Allow a bit of space before and after the first fret (the nut) and the last one
        int fretX;
        int textWidth;
        mPaintText.setTextSize((float) (mStringSpace / FRET_NUMBER_TEXT_DIVISOR));
        mPaintText.setColor(Color.GREEN);

        Log.d(TAG, "textSize="+mPaintText.getTextSize());
        while(fret<=mFrets){
            //Draw vertical line - from top to bottom string
            fretX = mSpaceBeforeNut+ (fret*mFretWidth);
            g.drawLine(fretX, mStringSpace, fretX, getHeight() - mStringSpace, mPaint);
            // Set up mPaint for the Fret number text
            textWidth= (int) mPaintText.measureText(""+(fret+1));
            // Write fret number above the top string
            float y = (mStringSpace/2);
            g.drawText(fret+"",fretX-(mFretWidth/2)-(textWidth/2),y, mPaintText);
            ++fret;
        }
    }
    private void drawNotes(Canvas g) {
        if(mFretNotes!=null) {
            Log.d(TAG, "drawNotes ["+mFretNotes.size()+"]");
            for (int i = 0; i < mFretNotes.size(); i++) {
                // draw a circle on the relevant string in the middle of the fret
                Log.d(TAG, "note ["+mFretNotes.get(i).note+"] string ["+mFretNotes.get(i).string+
                        "] fret["+mFretNotes.get(i).fret+"] name ["+mFretNotes.get(i).name+"]");
                g.drawCircle(getNoteX(mFretNotes.get(i)), getNoteY(mFretNotes.get(i)), mStringSpace / 4, mPaintNote);
            }
        }
    }

    private float getNoteY(FretNote note) {
        // Get the y coord of the given string
        float ret = (note.string+1)*mStringSpace;
        Log.d(TAG, "getNoteY ["+ret+"]");
        return ret;
    }

    private float getNoteX(FretNote note) {
        // Get the centre of the given fret
        float ret;
        if( note.fret>0) {
            ret = (float) (mSpaceBeforeNut + ((note.fret - 1 + 0.5) * mFretWidth));
        }
        else{
            ret = (float) mSpaceBeforeNut;
        }
        Log.d(TAG, "getNoteX ["+ret+"]");
        return ret;
    }

    public void setNotes(List<FretNote> notes){
        mFretNotes = notes;
    }
    void testDrawNotes(){
        List<FretNote> myFretNotes = new ArrayList<>();
        myFretNotes.add(new FretNote(40, true, 5, 0, "E"));
        myFretNotes.add(new FretNote(46, true, 4, 1, "Bb"));
        myFretNotes.add(new FretNote(52, true, 3, 2, "E"));
        myFretNotes.add(new FretNote(58, true, 2, 3, "Bb"));
        myFretNotes.add(new FretNote(63, true, 1, 4, "Eb"));
        myFretNotes.add(new FretNote(69, true, 0, 5, "A"));

        Log.d(TAG, "b4 getFretPositions");
        setNotes(myFretNotes);
    }
}