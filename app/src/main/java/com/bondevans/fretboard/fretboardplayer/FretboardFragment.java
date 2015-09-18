package com.bondevans.fretboard.fretboardplayer;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.R.layout.simple_spinner_item;

/**
 * A placeholder fragment containing a simple view.
 */
public class FretboardFragment extends Fragment {
    private static final String TAG = "FretboardFragment";
    private static final int MAX_STRINGS_GUITAR = 6;
    private static final int[] GUITAR_STANDARD_TUNING = new int[]{64,59,55,50,45,40};   // Highest to lowest
    private static final String[] GUITAR_STANDARD_TUNING_STRING_NAMES = new String[]{"Top E","B","G","D","A","Low E"};
    private static final int MAX_FRETS_GUITAR = 20;
    private FretboardView mFretboardView;
    private List<String> mTrackNames = new ArrayList<>();
    private List<FretEvent> mFretEvents;
    private MidiFile mMidiFile;
    private TextView mFileNameText;
    private String mFileName = "/sdcard/midi/test.mid";
    private Spinner mTrackSpinner;
    private Button mPlayButton;
    ArrayAdapter<String> mTrackAdapter;
    private FretEventHander mFretEventHandler;

    public FretboardFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.fretboard_fragment_layout, container, false);
        mFretboardView = (FretboardView) myView.findViewById(R.id.fretboard);
        mTrackSpinner = (Spinner) myView.findViewById(R.id.track_spinner);
        mTrackSpinner.setEnabled(false);
        mFileNameText = (TextView) myView.findViewById(R.id.file_name);
        mFileNameText.setText(mFileName);

        // sort out Play button
        mPlayButton = (Button) myView.findViewById(R.id.play_button);
        mPlayButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "On Button Click : " + String.valueOf(mTrackSpinner.getSelectedItem()));
                mFretEvent=0;
                mFretEventHandler.sleep(mFretEvents.get(mFretEvent).timeDelay);
            }
        });
        mFretEventHandler = new FretEventHander();
        // Load up Midi file header
        LoadFileHeaderTask loadfile = new LoadFileHeaderTask(mFileName);// Do this in background
        loadfile.execute();
        Log.d(TAG, "onCreate END");
        return myView;
    }

    void setupTrackSpinner(){
        mTrackAdapter = new ArrayAdapter<String>
                (this.getActivity(), simple_spinner_item,mTrackNames);

        mTrackAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        mTrackSpinner.setAdapter(mTrackAdapter);
        // Spinner item selection Listener
        mTrackSpinner.setOnItemSelectedListener(new trackSelectedListener());
        mTrackSpinner.setEnabled(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
    }

    /**
     * Calculates the time between FretEvents from the delta time (clicks) and the mTicks per beat setting
     * in the file
     *
     * @param ticksPerBeat
     * @param deltaTime
     * @return
     */
    private long delayFromClicks(int ticksPerBeat, int deltaTime) {
        long ret=60000/(ticksPerBeat*deltaTime);
        Log.d(TAG, "timeDelay=[" + ret + "]");
        return ret;
    }

    class LoadFileHeaderTask extends AsyncTask<Void, Integer, Void> {
        String midiFilePath;

        public LoadFileHeaderTask(String midiFilePath){
            this.midiFilePath = midiFilePath;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "Loading header onPreExecute");
            // TODO Show progress bar
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "Loading header: "+midiFilePath);
            try {
                mMidiFile = new MidiFile(midiFilePath);
                mTrackNames = mMidiFile.getTrackNames();
            } catch (FretBoardException e) {
                Log.d(TAG, e.getMessage());
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "Loading header onPostExecute");
            for(String x: mTrackNames){
                Log.d(TAG,"Trackname: "+x);
            }
            setupTrackSpinner();
        }
    }

    /**
     * Loads up the note events for the selected track.
     */
    class LoadTrackTask extends AsyncTask<Void, Integer, Void> {
        int     track;

        public LoadTrackTask(int track){
            this.track = track;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "Loading track onPreExecute");
            mPlayButton.setEnabled(false);
            // TODO Show progress bar
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "Loading track: "+track);
            // convert MIDI file into list of fretboard events
            List<NoteEvent> noteEvents;
            try {
                noteEvents = mMidiFile.loadNoteEvents(track);
                Log.d(TAG, "Got "+ noteEvents.size()+" events");
            } catch (FretBoardException | IOException e) {
                Log.d(TAG, e.getMessage());
                return null;
            }
            //  Loop through list of events.  For each set of events with the same time (i.e. chords)
            //  get the fret positions
            Log.d(TAG, "Loading Fret Events");
            mFretEvents = new ArrayList<>();
            boolean first=true;
            FretPosition fp = new FretPosition(MAX_STRINGS_GUITAR, MAX_FRETS_GUITAR,
                    GUITAR_STANDARD_TUNING, GUITAR_STANDARD_TUNING_STRING_NAMES );
            List<FretNote> fretNotes = new ArrayList<>();
            int count=0;
            long timeDelay=0;
            for(NoteEvent ev : noteEvents){
                if(!first && ev.deltaTime>0){
                    // If we get an event with a delay then
                    // get fret positions for the previous set,
                    // reset count to zero and
                    // start building the next set.
                    Log.d(TAG, "Getting positions for ["+count+"] notes");
                    fretNotes = fp.getFretPositions(fretNotes);
                    mFretEvents.add(new FretEvent(timeDelay, fretNotes));
                    // calculate delay time and save for later
                    timeDelay = delayFromClicks(mMidiFile.getTicksPerBeat(), ev.deltaTime);
                    //reset the list of notes
                    fretNotes = new ArrayList<>();
                    count=0;
                }
                Log.d(TAG, "Got event [" + ev.on + "][" + ev.deltaTime + "][" + ev.note + "]");
                first=false;
                fretNotes.add(new FretNote(ev.note, ev.on));
                count++;
            }
            // Don't forget the last one
            if(!first){
                Log.d(TAG, "Getting positions for ["+count+"] notes (Last one)");
                mFretEvents.add(new FretEvent(timeDelay, fp.getFretPositions(fretNotes)));
            }
            Log.d(TAG, "Got ["+mFretEvents.size()+"] FretEvents");
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(TAG, "Loading track onPostExecute");
            super.onPostExecute(aVoid);
            if( mFretEvents.size()>0) {
                mPlayButton.setEnabled(true);
            }
            else{
                Log.d(TAG, "onPostExecute - No notes in this track");
                Toast.makeText(getActivity(), "No notes in this track", Toast.LENGTH_LONG).show();
            }
        }
    }

    class trackSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            // Load up selected track
            Log.d(TAG, "Track "+pos + " selected");
            LoadTrackTask loadtrack = new LoadTrackTask(pos);// Do this in background
            loadtrack.execute();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            Log.d(TAG, "onNothingSelected");
        }
    }

    private int mFretEvent=0;
    class FretEventHander extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage fretEvent[" + mFretEvent + "]");
            // Send next event to
            mFretboardView.setNotes(mFretEvents.get(mFretEvent).fretNotes);
            mFretboardView.invalidate();
            if(++mFretEvent<mFretEvents.size()) {
                Log.d(TAG, "setting up fretEvent :"+mFretEvent);
                sleep(mFretEvents.get(mFretEvent).timeDelay);
            }
            else{
                Log.d(TAG, "No more fretEvents");
            }
        }

        public void sleep(long delayMillis) {
            Log.d(TAG, "sleep [" + delayMillis + "]");
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }
}
