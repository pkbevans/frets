package com.bondevans.frets.freteditor;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.bondevans.frets.R;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.fretview.FretView;
import com.bondevans.frets.midiService.MidiService.OnSendMidiListener;

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
    private boolean mEdited = false;
    private TextView mEventText;
    private OnSendMidiListener mOnSendMidiListener;
    private boolean mInstrumentSet=false;
    private int mTracksize;
    private int displayEvent = 0;
    private Switch mFollowNoteSwitch;

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
        HorizontalScrollView scrollView = myView.findViewById(R.id.horizontalScrollView);
        mFretEditView = myView.findViewById(R.id.fretview);
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
        mTrackName = myView.findViewById(R.id.track_name);

        Button nextButton = myView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ++displayEvent;
                gotoNote(NOTE_NEXT);
            }
        });
        Button prevButton = myView.findViewById(R.id.prev_button);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                --displayEvent;
                // Goto previous note
                gotoNote(NOTE_PREV);
            }
        });
        Button upButton = myView.findViewById(R.id.up_button);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mFretTrack.moveNotes(mCurrentEvent,true)){
                    mEdited = true;
                }
                mFretEditView.invalidate();
            }
        });

        Button downButton = myView.findViewById(R.id.down_button);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mFretTrack.moveNotes(mCurrentEvent,false)){
                    mEdited = true;
                }
                mFretEditView.invalidate();
            }
        });

        mEventText = myView.findViewById(R.id.event);
        mFollowNoteSwitch = myView.findViewById(R.id.followNoteSwitch);

        mFollowNoteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mFretTrack.setFollowNotes(isChecked);
            }
        });
        if (savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState != null");
            // Must be orientation change
            mTrackName.setText(mFretTrack.getName());
            mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
            setEventText(displayEvent);
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
                    mCurrentEvent = displayEvent = 1;
                }
            } else {
                --mCurrentEvent;
                if (mCurrentEvent < 0) {
                    mCurrentEvent = mFretTrack.fretEvents.size() - 1;
                    displayEvent = mTracksize;
                }
            }
        }
        while ( !mFretTrack.fretEvents.get(mCurrentEvent).hasOnNotes() ||
                (mFretTrack.isMerged() &&
                mFretTrack.fretEvents.get(mCurrentEvent).track!=mSoloTrack));

        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
        mFretEditView.invalidate();
        setEventText(displayEvent);
        // Play the note(s) as well
        // Set Midi Instrument if first time
        if(!mInstrumentSet) {
            mOnSendMidiListener.setMidiInstrument(DEFAULT_CHANNEL, mFretTrack.getMidiInstrument());
            mInstrumentSet= true;
        }
        mOnSendMidiListener.sendMidiNotes(mFretTrack.fretEvents.get(mCurrentEvent),DEFAULT_CHANNEL, NOTE_LENGTH);
    }

    private void setEventText(int ev){
        mEventText.setText(getString(R.string.event_count,(ev), mTracksize));
    }
    /**
     * Set the <code>FretTrack</code>
     *
     * @param fretTrack Fret Track
     */
    public void setFretTrack(FretTrack fretTrack, int track) {
        Log.d(TAG, "setFretTrack");
        mFretTrack = fretTrack;
        mSoloTrack = track;
        mTracksize = mFretTrack.getEventSizeForTrack(mSoloTrack);
        mTrackName.setText(mFretTrack.getName());
        mCurrentEvent = 0;
        mFretEditView.setFretInstrument(mFretTrack.getInstrument());
        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent));
        mFretEditView.invalidate();
        setEventText(displayEvent);
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
                Log.d(TAG, "groupNotesAtFret: "+fret);
                mFretTrack.groupNotesAtFret(fret);
                mFretEditView.invalidate();
                mEdited=true;            }
        });
        groupNotesAtFretDialog.show(getActivity().getSupportFragmentManager(),GROUP_NOTES);
    }
}
