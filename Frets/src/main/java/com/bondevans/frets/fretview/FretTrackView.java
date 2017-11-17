package com.bondevans.frets.fretview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**¬
 * <code>FretTrackView</code> extends <code>FretView</code> to support tempo change
 */
public class FretTrackView extends FretView {
    private static final String TAG = FretTrackView.class.getSimpleName();
    private static final int MINIMUM_TEMPO = 10;
    private static final int DEFAULT_TEMPO = 120;
    private int mCurrentTempo;
    private GestureDetector gestureDetector;
    private FretListener fretListener;

    public interface FretListener {
        void OnTempoChanged(int tempo);
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
    }

    @Override
    protected void onDraw(Canvas g) {
        super.onDraw(g);
        doTempo();
    }

    private void doTempo() {
        if (mCurrentTempo == 0) {
            mCurrentTempo = DEFAULT_TEMPO;  // Set it to default instrument first time
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
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

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "onSingleTapConfirmed");
            return super.onSingleTapConfirmed(e);
        }
    }

    private static final int DISTANCE_FACTOR = 5;

    private void updateTempo(float distanceY) {

        int x = (int) distanceY;
        int b = x / DISTANCE_FACTOR;
        mCurrentTempo += b;
        // Don't go below MINIMUM Tempo
        if (mCurrentTempo < MINIMUM_TEMPO) {
            mCurrentTempo = MINIMUM_TEMPO;
        }
//        Log.d(TAG, "updateTempo ["+b+"] currentTempo ["+mCurrentTempo+"]");
        invalidate();
        fretListener.OnTempoChanged(mCurrentTempo);
    }
}