package com.bondevans.fretboard.freteditor;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.fretview.FretEvent;
import com.bondevans.fretboard.fretview.FretNote;
import com.bondevans.fretboard.fretview.FretPosition;
import com.bondevans.fretboard.fretview.FretSong;
import com.bondevans.fretboard.fretview.FretTrack;
import com.bondevans.fretboard.instruments.FretGuitarStandard;

import java.util.List;

public class FretEditFragment extends Fragment {
    private static final String TAG = FretEditFragment.class.getSimpleName();
    private static final int NOTE_NEXT = 1;
    private static final int NOTE_PREV = 2;
    private FretEditView mFretEditView;
    private TextView mSongName;
    private TextView mTrackName;
    private FretTrack mFretTrack;
    private String mSong;
    private int mCurrentEvent = 0;
    private FretPosition mFretPosition;
    private boolean mEdited = false;
    private FretSong mFretSong;

    public FretEditFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "onCreate");
        mFretPosition = new FretPosition(new FretGuitarStandard());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.fretedit_layout, container, false);
        HorizontalScrollView scrollView = (HorizontalScrollView) myView.findViewById(R.id.horizontalScrollView);
        mFretEditView = (FretEditView) myView.findViewById(R.id.fretview);
        scrollView.setOnTouchListener(mFretEditView);
        mSongName = (TextView) myView.findViewById(R.id.song_name);
        mTrackName = (TextView) myView.findViewById(R.id.track_name);
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
            mSongName.setText(mSong);
            mTrackName.setText(mFretTrack.getName());
            mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent).fretNotes);
        }
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
                if (++mCurrentEvent >= mFretTrack.fretEvents.size()) {
                    mCurrentEvent = 0;
                }
            } else {
                if (--mCurrentEvent < 0) {
                    mCurrentEvent = mFretTrack.fretEvents.size() - 1;
                }
            }
        }
        while (!mFretTrack.fretEvents.get(mCurrentEvent).hasOnNotes());

        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent).fretNotes);
        mFretEditView.invalidate();
    }

    /**
     * Set the song to view - nothing shown until we supply a song to view
     *
     * @param song      Song name
     * @param fretTrack Track to edit
     */
    private void setFretTrack(String song, FretTrack fretTrack) {
        Log.d(TAG, "setFretTrack");
        mSong = song;
        mSongName.setText(song);
        mTrackName.setText(fretTrack.getName());
        mFretTrack = fretTrack;
        mCurrentEvent = 0;
        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent).fretNotes);
    }

    public void setFretSong(FretSong fretSong) {
        mFretSong = fretSong;
        setFretTrack(mFretSong.getName(), fretSong.getTrack(0));
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

    public boolean isEdited() {
        return mEdited;
    }

    public FretSong getFretSong() {
        return mFretSong;
    }
}
