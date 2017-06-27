package com.bondevans.frets.freteditor;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;

import com.bondevans.frets.R;
import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.fretview.FretPosition;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.instruments.FretGuitarStandard;

import java.util.List;

public class FretTrackEditFragment extends Fragment {
    private static final String TAG = FretTrackEditFragment.class.getSimpleName();
    private static final int NOTE_NEXT = 1;
    private static final int NOTE_PREV = 2;
    private FretEditView mFretEditView;
    private EditText mTrackName;
    FretTrack mFretTrack;
    private int mCurrentEvent = 0;
    private FretPosition mFretPosition;
    private boolean mEdited = false;

    public FretTrackEditFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);  //Doesn't work for fragments added to the backstack
        Log.d(TAG, "onCreate");
        mFretPosition = new FretPosition(new FretGuitarStandard());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.frettrackedit_layout, container, false);
        HorizontalScrollView scrollView = (HorizontalScrollView) myView.findViewById(R.id.horizontalScrollView);
        mFretEditView = (FretEditView) myView.findViewById(R.id.fretview);
        mFretEditView.setKeepScreenOn(true);
        scrollView.setOnTouchListener(mFretEditView);
        mTrackName = (EditText) myView.findViewById(R.id.track_name);

        Button nextButton = (Button) myView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoNote(NOTE_NEXT);
            }
        });
        Button prevButton = (Button) myView.findViewById(R.id.prev_button);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Goto previous note
                gotoNote(NOTE_PREV);
            }
        });
        Button upButton = (Button) myView.findViewById(R.id.up_button);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveNotes(true);
                mFretEditView.invalidate();
            }
        });

        Button downButton = (Button) myView.findViewById(R.id.down_button);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveNotes(false);
                mFretEditView.invalidate();
            }
        });

        if (savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState != null");
            // Must be orientation change
            mTrackName.setText(mFretTrack.getName());
            mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
        }
        Log.d(TAG, "onCreateView-end");
        return myView;
    }

    /**
     * Move backwards or forwards thru the notes
     *
     * @param nextPrev <code>NOTE_NEXT</code>=NEXT <code>NOTE_PREV</code>=PREV
     */
    private void gotoNote(int nextPrev) {
        // Get next note
        do {
            if (nextPrev == NOTE_NEXT) {
                ++mCurrentEvent;
                if (mCurrentEvent >= mFretTrack.fretEvents.size()) {
                    mCurrentEvent = 0;
                }
            } else {
                --mCurrentEvent;
                if (mCurrentEvent < 0) {
                    mCurrentEvent = mFretTrack.fretEvents.size() - 1;
                }
            }
        }
        while (//mFretTrack.fretEvents.get(mCurrentEvent).track!=mTrack||
                !mFretTrack.fretEvents.get(mCurrentEvent).hasOnNotes());

        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
        mFretEditView.invalidate();
    }

    /**
     * Set the <code>FretTrack</code>
     *
     * @param fretTrack Fret Track
     */
    public void setFretTrack(FretTrack fretTrack) {
        Log.d(TAG, "setFretTrack");
        mFretTrack = fretTrack;
        mTrackName.setText(mFretTrack.getName());
        mCurrentEvent = 0;
        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
    }

    /**
     * Moves current note(s) to an adjacent string
     *
     * @param up note is moved to a higher string if true, lower if false
     */
    private void moveNotes(boolean up) {
        // Get current event
        FretEvent fretEvent = mFretTrack.fretEvents.get(mCurrentEvent);
        // Get the list of notes at current event
        List<FretNote> notes = fretEvent.fretNotes;
        // For each note, get the new position
        for (FretNote fretNote : notes) {
            // Ignore OFF NOTES
            if (fretNote.on) {
                // Get the new position
                Log.d(TAG, "moveNotes OLD:" + fretNote.toString());
                mFretPosition.moveNoteOneString(fretNote, up);
                Log.d(TAG, "moveNotes NEW:" + fretNote.toString());
                mEdited = true;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Returns true if the track (name or notes) have been updated.
     * @return true/false
     */
    public boolean isEdited() {
        if (mTrackName.getText().toString().compareTo(mFretTrack.getName())!=0) {
            mFretTrack.setName(mTrackName.getText().toString());
            mEdited=true;
        }
        return mEdited;
    }
}
