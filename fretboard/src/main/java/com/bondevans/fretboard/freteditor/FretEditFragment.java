package com.bondevans.fretboard.freteditor;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.fretview.FretEvent;
import com.bondevans.fretboard.fretview.FretNote;
import com.bondevans.fretboard.fretview.FretPosition;
import com.bondevans.fretboard.fretview.FretSong;
import com.bondevans.fretboard.fretview.FretTrack;
import com.bondevans.fretboard.instruments.FretGuitarStandard;
import com.bondevans.fretboard.utils.FileLoaderTask;

import java.io.File;
import java.util.List;

import static android.R.layout.simple_spinner_item;

public class FretEditFragment extends Fragment {
    private static final String TAG = FretEditFragment.class.getSimpleName();
    private static final int NOTE_NEXT = 1;
    private static final int NOTE_PREV = 2;
    private FretEditView mFretEditView;
    private EditText mSongNameText;
    private CheckBox mSoloTrack;
    private FretTrack mFretTrack;
    private String mSongName;
    private int mCurrentEvent = 0;
    private FretPosition mFretPosition;
    private boolean mEdited = false;
    public FretSong mFretSong;
    private static final int NO_TRACK_SELECTED = -1;
    private Spinner mTrackSpinner;
    ArrayAdapter<String> mTrackAdapter;
    private int mSelectedTrack = NO_TRACK_SELECTED;
    private ProgressDialog progressDialog;

    public FretEditFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "onCreate");
        mFretPosition = new FretPosition(new FretGuitarStandard());
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

            }
        });
        progressDialog.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.fretedit_layout, container, false);
        HorizontalScrollView scrollView = (HorizontalScrollView) myView.findViewById(R.id.horizontalScrollView);
        mFretEditView = (FretEditView) myView.findViewById(R.id.fretview);
        scrollView.setOnTouchListener(mFretEditView);
        mSongNameText = (EditText) myView.findViewById(R.id.song_name);
        mSoloTrack = (CheckBox) myView.findViewById(R.id.checkBox);
        mSoloTrack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Solo button changed:" + (isChecked ? " TRUE" : " FALSE"));
                makeCurrentTrackSolo(isChecked);
            }
        });
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
        mTrackSpinner = (Spinner) myView.findViewById(R.id.track_spinner);
        mTrackSpinner.setEnabled(false);

        ImageButton deleteTrackBtn = (ImageButton) myView.findViewById(R.id.deleteTrack);
        deleteTrackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "deleteTrack Button clicked");
                deleteCurrentTrack();
            }
        });
        ImageButton editTrackBtn = (ImageButton) myView.findViewById(R.id.editTrackName);
        editTrackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "editTrack Button clicked");
                deleteCurrentTrack();
            }
        });

        if (savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState != null");
            // Must be orientation change
            setupTrackSpinner();
            mSongNameText.setText(mSongName);
            mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent).fretNotes, mFretTrack.fretEvents.get(mCurrentEvent).bend);
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

        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent).fretNotes, mFretTrack.fretEvents.get(mCurrentEvent).bend);
        mFretEditView.invalidate();
    }

    /**
     * Set the song to view - nothing shown until we supply a song to view
     *
     * @param fretTrack Track to edit
     */
    private void setFretTrack(FretTrack fretTrack) {
        Log.d(TAG, "setFretTrack");
        mFretTrack = fretTrack;
        mCurrentEvent = 0;
        Log.d(TAG, "Selected track=" + mSelectedTrack);
        mSoloTrack.setChecked(mSelectedTrack == mFretSong.getSoloTrack());
        // Can't de-select the first track as the solo track - you have to positively select
        // the other track as solo - always has t be a solo track and default is zero(first)
        mSoloTrack.setEnabled((mFretSong.tracks() > 0) && !(mSelectedTrack == 0 && mSoloTrack.isChecked()));
        mFretEditView.setNotes(mFretTrack.fretEvents.get(mCurrentEvent).fretNotes, mFretTrack.fretEvents.get(mCurrentEvent).bend);
    }

    public void setFretSong(FretSong fretSong) {
        mSongName = fretSong.getName();
        mSongNameText.setText(fretSong.getName());
        mFretSong = fretSong;
        setupTrackSpinner();
        mSelectedTrack = 0;
        setFretTrack(fretSong.getTrack(mSelectedTrack));    // Start off with first track
    }

    public void setFretSong(File file) {
        progressDialog.show();
        FileLoaderTask fileLoaderTask = new FileLoaderTask(file);
        fileLoaderTask.setFileLoadedListener(new FileLoaderTask.FileLoadedListener() {
            @Override
            public void OnFileLoaded(String contents) {
                setFretSong(new FretSong(contents));
                progressDialog.hide();
            }

            @Override
            public void OnError(String msg) {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        });
        fileLoaderTask.execute();
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
        if (!mSongNameText.getText().toString().equalsIgnoreCase(mFretSong.getName())) {
            mFretSong.setName(mSongNameText.getText().toString());
            mEdited = true;
        }
        return mEdited;
    }

    public FretSong getFretSong() {
        return mFretSong;
    }

    public void makeCurrentTrackSolo(boolean solo) {
        if ((!solo && mSelectedTrack != mFretSong.getSoloTrack()) || (solo && mSelectedTrack == mFretSong.getSoloTrack())) {
            // Dont update if current track is already NOT the solo track and solo is false OR
            // solo is true and current track ALREADY is the solo track.
            Log.d(TAG, "NOT updating solo");
            return;
        }
        mFretSong.setSoloTrack(solo ? mSelectedTrack : 0);
        mEdited = true;
    }

    public void deleteCurrentTrack() {
        mFretSong.deleteTrack(mSelectedTrack);
        setFretTrack(mFretSong.getTrack(mFretSong.getSoloTrack()));
        // Set up spinner again (invalidate??)
        setupTrackSpinner();
        mEdited = true;
    }

    private void setupTrackSpinner() {
        mTrackAdapter = new ArrayAdapter<>
                (this.getActivity(), simple_spinner_item, mFretSong.getTrackNames());

        mTrackAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        mTrackSpinner.setAdapter(mTrackAdapter);
        // Spinner item selection Listener
        mTrackSpinner.setOnItemSelectedListener(new TrackSelectedListener());
        if (mFretSong != null) {
            mTrackSpinner.setEnabled((mFretSong.tracks() > 1));
        }
    }

    class TrackSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Log.d(TAG, "TrackSelectedListener: " + pos + " selected");
            // Load up selected track
            String track = (String) parent.getItemAtPosition(pos);
            if (mSelectedTrack == pos) {
                Log.d(TAG, "ORIENTATION CHANGE Track " + pos + " selected: " + track);
                // must be orientation change - ignore
            } else {
                mSelectedTrack = pos;
                // Must be different track selected in current session
                Log.d(TAG, "CHANGE Track " + pos + " selected: " + track);
                setFretTrack(mFretSong.getTrack(pos));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            Log.d(TAG, "onNothingSelected");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        progressDialog.dismiss();
    }
}
