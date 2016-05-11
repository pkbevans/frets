package com.bondevans.frets.fretviewer;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrackView;
import com.bondevans.frets.utils.FileLoaderTask;

import org.billthefarmer.mididriver.MidiDriver;

import java.io.File;

public class FretViewFragment extends Fragment implements MidiDriver.OnMidiStartListener {
    private static final String TAG = FretViewFragment.class.getSimpleName();
    private static final int ALTO_SAXOPHONE = 65;
    private static final int XYLOPHONE = 13;
    private static final int ELECTRIC_GUITAR = 29;
    private FretTrackView mFretTrackView;
    private TextView mSongName;
    private TextView mTrackName;
    private ImageButton playPauseButton;
    private SeekBar mSeekBar;
    private TextView mTempoText;
    private int mTempo;
    private int mCurrentEvent;
    private FretSong mFretSong;
    private ProgressDialog progressDialog;
    private Drawable playDrawable;
    private Drawable pauseDrawable;
    private boolean mPlay;
    private MidiDriver mMidiDriver;

    public FretViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "onCreate");
        pauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.pausebutton2);
        playDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.playbutton2);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

            }
        });
        progressDialog.show();
        // Instantiate the driver.
        mMidiDriver = new MidiDriver();
        // Set the listener.
        mMidiDriver.setOnMidiStartListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.fretview_layout, container, false);
        mTrackName = (TextView) myView.findViewById(R.id.track_name);
        mFretTrackView = (FretTrackView) myView.findViewById(R.id.fretview);
        mFretTrackView.setFretListener(new FretTrackView.FretListener() {
            @Override
            public void OnProgressUpdated(int length, int current) {
                mCurrentEvent = current;
                mSeekBar.setProgress(current * 100 / length);
            }

            @Override
            public void OnTempoChanged(int tempo) {
                mTempo = tempo;
                mTempoText.setText(String.valueOf(tempo));
            }

            @Override
            public void OnPlayEnabled(boolean flag) {
                mPlay = true;
                progressDialog.hide();
                // Probably need to set up instrument here
                setMidiInstrument(ELECTRIC_GUITAR);
            }

            @Override
            public void SendMidi(byte[] buffer) {
                mMidiDriver.write(buffer);
            }
        });
        mTempoText = (TextView) myView.findViewById(R.id.bpmText);
        mSongName = (TextView) myView.findViewById(R.id.song_name);
        mSeekBar = (SeekBar) myView.findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                Log.d(TAG, "onProgressChanged [" + progress + "] " + (fromUser ? "FROM USER" : "" + ""));
                // If user moves the position, then need to update current fretEvent in Fretboard view
                // progress = a value between 0-100 - e.g. percent of way through the song
                if (fromUser) {
                    mFretTrackView.moveTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        playPauseButton = (ImageButton) myView.findViewById(R.id.playPauseButton);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle Play/pause
                mPlay = !mPlay;
                playPauseButton.setImageDrawable(mPlay ? playDrawable : pauseDrawable);
                mFretTrackView.play();
            }
        });
        if (savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState != null");
            // Must be orientation change
            mSongName.setText(mFretSong.getName());
            mFretTrackView.setTrack(mFretSong.getTrack(mFretSong.getSoloTrack()), mFretSong.getTpqn(), mTempo, mCurrentEvent);
        }
        return myView;
    }

    /**
     * Set the song to view - nothing shown until we supply a song to view
     *
     * @param fretSong Song to view
     */
    private void setFretSong(FretSong fretSong) {
        Log.d(TAG, "setFretSong");
        mFretSong = fretSong;
        mSongName.setText(mFretSong.getName());
        mTrackName.setText(mFretSong.getTrackName(mFretSong.getSoloTrack()));
        mFretTrackView.setTrack(mFretSong.getTrack(mFretSong.getSoloTrack()), mFretSong.getTpqn(), mFretSong.getBpm());
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

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // The app is losing focus so need to stop the
        mMidiDriver.stop();
        mFretTrackView.pause();
        progressDialog.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mMidiDriver.start();
        // Get the configuration.
        int[] config = mMidiDriver.config();

        // Print out the details.
        Log.d(TAG, "maxVoices: " + config[0]);
        Log.d(TAG, "numChannels: " + config[1]);
        Log.d(TAG, "sampleRate: " + config[2]);
        Log.d(TAG, "mixBufferSize: " + config[3]);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onMidiStart() {
        Log.d(TAG, "onMidiStart()");
    }

    private void setMidiInstrument(int instrument) {
        Log.d(TAG, "setMidiInstrument: " + instrument);
        byte[] event = new byte[2];
        event[0] = (byte) (0xC0 | 0);// channel hardcoded to 0
        event[1] = (byte) instrument;
        mMidiDriver.write(event);
    }
}
