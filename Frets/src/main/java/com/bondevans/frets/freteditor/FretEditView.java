package com.bondevans.frets.freteditor;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.bondevans.frets.fretview.FretView;

public class FretEditView extends FretView implements View.OnTouchListener {
    private static final String TAG = FretEditView.class.getSimpleName();
    private GestureDetector gestureDetector;

    public FretEditView(Context context) {
        super(context);
        initialiseView(context);
    }

    /**
     * Constructor
     */
    public FretEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseView(context);
    }

    /**
     * All initialisation stuff
     *
     * @param context Context
     */
    private void initialiseView(Context context) {
        Log.d(TAG, "InitialiseView");
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
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Log.d(TAG, "onTouch UP");
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "onTouch DOWN");
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Log.d(TAG, "onTouch MOVE");
        } else if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            Log.d(TAG, "onTouch SCROLL");
        } else {
            Log.d(TAG, "onTouch - something else!!!!");
        }

        return false;
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "LONG PRESS");
            super.onLongPress(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown: ");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "HELLO onScroll distanceX=" + distanceX + "] distanceY=[" + distanceY + "]");
            return true;
        }
    }
}
