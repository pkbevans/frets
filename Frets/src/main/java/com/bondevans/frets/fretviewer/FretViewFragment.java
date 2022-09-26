package com.bondevans.frets.fretviewer;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrackView;

import org.billthefarmer.mididriver.MidiDriver;

import java.util.List;

import androidx.core.content.ContextCompat;

public class FretViewFragment extends Fragment implements MidiDriver.OnMidiStartListener {
    private static final String TAG = FretViewFragment.class.getSimpleName();
    private FretTrackView mFretTrackView;
    private ImageButton playPauseButton;
    private SeekBar mSeekBar;
    private TextView mTempoText;
    private int mTempo;
    private int mCurrentEvent;
    private FretSong mFretSong;
    private Drawable playDrawable;
    private Drawable pauseDrawable;
    private boolean mPlaying;
    private MidiDriver mMidiDriver;
    private FretPlayer mFretPlayer;
    private MidiReceiver mMidiReceiver;
    private final boolean mOldMidi=false;

    public FretViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "onCreate");
        pauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.pausebutton2);
        playDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.playbutton2);
        if(mOldMidi) {
            // Instantiate the driver.
            mMidiDriver = new MidiDriver();
            // Set the listener.
            mMidiDriver.setOnMidiStartListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.fretview_layout, container, false);
        mFretTrackView = myView.findViewById(R.id.fretview);
        mFretTrackView.setFretListener(new FretTrackView.FretListener() {
            @Override
            public void OnTempoChanged(int tempo) {
                mTempo = tempo;
                mFretPlayer.setTempo(tempo);
                mTempoText.setText(String.valueOf(tempo));
            }
        });
        mFretTrackView.setKeepScreenOn(true);

        mTempoText = myView.findViewById(R.id.bpmText);
        mSeekBar = myView.findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // If user moves the position, then need to update current fretEvent in Fretboard view
                // progress = a value between 0-100 - e.g. percent of way through the song
                if (fromUser) {
                    moveTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        playPauseButton = myView.findViewById(R.id.playPauseButton);
        playPauseButton.setVisibility(View.INVISIBLE);
        playPauseButton.setOnClickListener(v -> {
            play(); // Toggle Play/pause
        });
        if (savedInstanceState != null) {
            Log.d(TAG, "HELLO savedInstanceState != null tempo="+mTempo+" curent event="+mCurrentEvent);
            // Must be orientation change
            setupPlayer(mMidiReceiver, mFretSong.getTpqn(), mTempo, mCurrentEvent);
        }
        return myView;
    }

    /**
     * Set the song to view - nothing shown until we supply a song to view
     *
     * @param fretSong Song to view
     */
    public void setFretSong(FretApplication app, MidiReceiver midiReceiver, FretSong fretSong) {
        Log.d(TAG, "setFretSong");
        mMidiReceiver = midiReceiver;
        mFretSong = fretSong;
        mFretPlayer = new FretPlayer(new FretPlayer.OnUiUpdateRequiredListener() {
            @Override
            public void onClickEvent(int clickEvent, int currentEvent) {
                mCurrentEvent = currentEvent;
                updateProgress(mFretSong.getClickTrackSize(), clickEvent);
            }

            @Override
            public void onSoloTrackNoteEvent(List<FretNote> fretNotes, int bend) {
                mFretTrackView.setNotes(fretNotes, bend);
                mFretTrackView.invalidate();
            }

            @Override
            public void onTempoChange(int newTempo) {
                mTempo = newTempo;
            }
        }, app.mainThreadHandler, app.executorService, mFretSong);
        setupPlayer(midiReceiver, mFretSong.getTpqn(), mFretSong.getBpm(),0);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if(mOldMidi){
            mMidiDriver.stop();
        }
        // The app is losing focus so need to stop the player
        if(mPlaying) {
            // Playing so pause
            play();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if(mOldMidi) {
            mMidiDriver.start();
            // Get the configuration.
            int[] config = mMidiDriver.config();
            // Print out the details.
            Log.d(TAG, "maxVoices: " + config[0]);
            Log.d(TAG, "numChannels: " + config[1]);
            Log.d(TAG, "sampleRate: " + config[2]);
            Log.d(TAG, "mixBufferSize: " + config[3]);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }


    public FretSong getFretSong() {
        return mFretSong;
    }

    /**
     * Plays/pauses the current track
     */
    public void play() {
        // toggle play/pause
        mPlaying = !mPlaying;
        playPauseButton.setImageDrawable(mPlaying ? pauseDrawable: playDrawable );
        mFretPlayer.setPlaying(mPlaying);
    }

    public void updateProgress(int length, int current) {
//        Log.d(TAG, "HELLO updateProgress: "+current+" of "+length);
        mSeekBar.setProgress((current * 100 / length)+1 );
    }

    /**
     * Moves to new position in the midi event list - in response to user moving the seek bar
     *
     * @param progress - percentage through the song
     *
     */
    private void moveTo(int progress) {
        int event = mFretSong.getClickEventByClickNumber(mFretSong.getClickTrackSize() * progress/100);
        if (event < 0) {
            event = 0;
        }
        Log.d(TAG, "HELLO move to Event: [" + event + "]");
        mCurrentEvent=event;
        mFretPlayer.moveTo(event);
    }

    private void setupPlayer(MidiReceiver midiReceiver, int tpqn, int tempo, int currentFretEvent) {
        Log.d(TAG, "loadTrack");
        mTempo = tempo;
        mFretTrackView.setFretInstrument(mFretSong.getTrack(mFretSong.getSoloTrack()).getInstrument());
        mPlaying = false;
        mFretTrackView.invalidate();   // Force redraw
        mTempoText.setText(String.valueOf(tempo));
        mFretPlayer.setTrack(midiReceiver, tpqn, tempo, currentFretEvent);
        getActivity().runOnUiThread(() -> playPauseButton.setVisibility(View.VISIBLE));
    }

    @Override
    public void onMidiStart() {
        Log.d(TAG, "onMidiStart()");
    }
}
