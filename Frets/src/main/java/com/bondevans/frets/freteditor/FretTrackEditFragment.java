package com.bondevans.frets.freteditor;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import com.bondevans.frets.R;
import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.fretview.FretPosition;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.fretview.FretView;
import com.bondevans.frets.instruments.FretBassGuitarStandard;
import com.bondevans.frets.instruments.FretGuitarStandard;
import com.bondevans.frets.instruments.FretInstrument;
import com.bondevans.frets.midiService.MidiService.OnSendMidiListener;

import java.util.ArrayList;
import java.util.List;

public class FretTrackEditFragment extends Fragment {
    private static final String TAG = FretTrackEditFragment.class.getSimpleName();
    private static final int NOTE_NEXT = 1;
    private static final int NOTE_PREV = 2;
    private static final java.lang.String GROUP_NOTES = "grp";
    static final int DEFAULT_CHANNEL = 0;
    private static final int NOTE_LENGTH = 400;
    private FretEditView mFretEditView;
    private EditText mTrackName;
    FretTrack mFretTrack;
    private int mSoloTrack;
    private int mCurrentEvent = 0;
    private FretPosition mFretPosition;
    private boolean mEdited = false;
    private TextView mEventText;
    private OnSendMidiListener mOnSendMidiListener;
    private boolean mInstrumentSet=false;
    private int mTracksize;

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "HELLO onAttach");
        super.onAttach(context);
        try {
            mOnSendMidiListener = (OnSendMidiListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + OnSendMidiListener.class.getSimpleName());
        }
    }

    public FretTrackEditFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);  //Doesn't work for fragments added to the backstack
        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.frettrackedit_layout, container, false);
        HorizontalScrollView scrollView = (HorizontalScrollView) myView.findViewById(R.id.horizontalScrollView);
        mFretEditView = (FretEditView) myView.findViewById(R.id.fretview);
        mFretEditView.setKeepScreenOn(true);
        mFretEditView.setFretEditListener(new FretEditView.FretEditListener() {
            @Override
            public void OnFretSelected(int fret) {
                Log.d(TAG, "OnFretSelected: "+fret);
                // Show the GroupNotesAtFret dialog if fret != -1
                if(fret != FretView.BAD_FRET) {
                    showGroupNotesAtFretDialog(fret);
                }
            }
        });
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

        mEventText = (TextView)myView.findViewById(R.id.event);
        if (savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState != null");
            // Must be orientation change
            mTrackName.setText(mFretTrack.getName());
            mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
            setEventText();
            mInstrumentSet=false;
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
        while ( !mFretTrack.fretEvents.get(mCurrentEvent).hasOnNotes() ||
                (mFretTrack.isMerged() &&
                mFretTrack.fretEvents.get(mCurrentEvent).track!=mSoloTrack));

        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
        mFretEditView.invalidate();
        setEventText();
        // Play the note(s) as well
        // Set Midi Instrument if first time
        if(!mInstrumentSet) {
            mOnSendMidiListener.setMidiInstrument(DEFAULT_CHANNEL, mFretTrack.getMidiInstrument());
            mInstrumentSet= true;
        }
        mOnSendMidiListener.sendMidiNotes(mFretTrack.fretEvents.get(mCurrentEvent),DEFAULT_CHANNEL, NOTE_LENGTH);
    }

    private void setEventText(){
        mEventText.setText(getString(R.string.event_count,(mCurrentEvent+1), mTracksize));
    }
    /**
     * Set the <code>FretTrack</code>
     *
     * @param fretTrack Fret Track
     */
    public void setFretTrack(FretTrack fretTrack, int track) {
        Log.d(TAG, "setFretTrack");
        mFretTrack = fretTrack;
        mFretPosition = new FretPosition(mFretTrack.getFretInstrument());
        mSoloTrack = track;
        mTracksize = mFretTrack.getEventSizeForTrack(mSoloTrack);
        mTrackName.setText(mFretTrack.getName());
        mCurrentEvent = 0;
        FretInstrument.Instrument instrument;
        if(mFretTrack.getFretInstrument() == FretInstrument.INTRUMENT_GUITAR) {
            instrument = new FretGuitarStandard();
        } else{
            instrument = new FretBassGuitarStandard();
        }
        mFretEditView.setFretInstrument(instrument);
        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
        mFretEditView.invalidate();
        setEventText();
    }

    /**
     * Moves current note(s) to an adjacent string
     *
     * @param up note is moved to a higher string if true, lower if false
     */
    private void moveNotes(boolean up) {
        // Save current Note(s)
        List<Integer> saveNotes = new ArrayList(mFretTrack.fretEvents.get(mCurrentEvent).getOnNotes());
        int event = mCurrentEvent;
        // Move the current note(s) and also move the notes in the next event if they are the same
        while(event<mFretTrack.fretEvents.size() &&
                (saveNotes.equals(mFretTrack.fretEvents.get(event).getOnNotes()) ||
                        // Ignore events that only have off notes
                        !mFretTrack.fretEvents.get(event).hasOnNotes()) ) {
            // Get event
            FretEvent fretEvent = mFretTrack.fretEvents.get(event);
            // Get the list of notes at event
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
            ++event;
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
    private void showGroupNotesAtFretDialog(int fret) {

        GroupNotesAtFretDialog groupNotesAtFretDialog = new GroupNotesAtFretDialog();
        groupNotesAtFretDialog.setFret(fret);
        groupNotesAtFretDialog.setOkListener(new GroupNotesAtFretDialog.OkListener() {
            @Override
            public void onOkClicked(int fret) {
                groupNotesAtFret(fret);
            }
        });
        groupNotesAtFretDialog.show(getActivity().getSupportFragmentManager(),GROUP_NOTES);
    }

    private void groupNotesAtFret(int targetFret) {
        Log.d(TAG, "groupNotesAtFret: "+targetFret);
        // Go through the FretNotes
        for (FretEvent fretEvent: mFretTrack.fretEvents) {
            mFretPosition.getFretPositions(fretEvent.fretNotes, targetFret);
        }
        mFretEditView.invalidate();
        mEdited=true;
    }
}
