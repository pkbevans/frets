package com.bondevans.fretboard.fretboardplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

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
    private Note[] mNotes;
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
        mPaintText.setAlpha(TEXT_ALPHA);       //  Fret Number and string Note text is not full power
        mPaintText.setTextAlign(Paint.Align.LEFT);
        // Set up a Paint for the Note dots
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

    private void drawStrings(Canvas g) {
        Log.d(TAG, "drawStrings");
        int string=1, stringY;
        // Set up mPaintText for String text
        mPaintText.setTextSize(getHeight() / STRING_TEXT_DIVISOR);
        mPaintText.setColor(Color.BLUE);

        Log.d(TAG, "getHeight=" + getHeight() + " mStringSpace=" + mStringSpace);
        while(string<=mStrings){
            // Draw string
            stringY=string*mStringSpace;
            Log.d(TAG, "stringY="+stringY+" getWidth="+getWidth());
            g.drawLine(0, stringY, getWidth(), stringY, mPaint);
            // Write String note
            Log.d(TAG, "mStringText="+mStringText[string-1]+" textSize="+mPaintText.getTextSize()+" y="+(stringY+ mPaintText.getTextSize()/2));
            g.drawText(mStringText[string-1],0,stringY+ mPaintText.getTextSize()/2, mPaintText);
            ++string;
        }
    }

    private void drawFrets(Canvas g) {
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
            g.drawLine(fretX,mStringSpace,fretX,getHeight()-mStringSpace,mPaint);
            // Set up mPaint for the Fret number text
            textWidth= (int) mPaintText.measureText(""+(fret+1));
            // Write fret number above the top string
            float y = (mStringSpace/2);
            g.drawText(fret+"",fretX-(mFretWidth/2)-(textWidth/2),y, mPaintText);
            ++fret;
        }
    }
    private void drawNotes(Canvas g) {
        for(Note note: mNotes){
            // draw a circle on the relevant string in the middle of the fret
            g.drawCircle(getNoteX(note), getNoteY(note), mStringSpace/4, mPaintNote);
        }
    }

    private float getNoteY(Note note) {
        // Get the y coord of the given string
        return note.string*mStringSpace;
    }

    private float getNoteX(Note note) {
        // Get the centre of the given fret
        return (float) (mSpaceBeforeNut + ((note.fret+0.5)*mFretWidth));
    }

    public void setNotes(Note[] notes){
        mNotes=notes;
    }

    void testDrawNotes(){
        Note [] myNotes = new Note[6];
        myNotes[0]= new Note(6,40, 0, true, "E");
        myNotes[1]= new Note(5,47, 2, true, "B");
        myNotes[2]= new Note(4,52, 2, true, "E");
        myNotes[3]= new Note(3,56, 1, true, "G#");
        myNotes[4]= new Note(2,59, 0, true, "B");
        myNotes[5]= new Note(1,64, 0, true, "E");

        Log.d(TAG, "b4 setNotes");
        setNotes(myNotes);
    }
}