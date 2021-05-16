package com.bondevans.frets.fretviewer;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.fretview.FretTrackView;
import com.bondevans.frets.utils.Synth;

import org.billthefarmer.mididriver.MidiDriver;

import java.util.List;

import androidx.core.content.ContextCompat;

import static android.content.Context.MIDI_SERVICE;

public class FretViewFragmentNew extends Fragment implements MidiDriver.OnMidiStartListener {
    private static final String TAG = FretViewFragmentNew.class.getSimpleName();
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
    private FretTrack mFretTrack;
    private int mSoloTrack;
    private int mClickTrackSize;
    private FretApplication mApp;
    private MidiDriver mMidiDriver;
    private MidiManager mMidiManager;
    private MidiReceiver mMidiReceiver;
    private MidiDevice mOpenDevice;
    private MidiPlayer mMidiPlayer;
    private final boolean mOldMidi=false;
    MidiDeviceInfo mMidiDeviceInfo;
    MidiDeviceInfo.PortInfo mPortInfo;

    public FretViewFragmentNew() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "onCreate");
        pauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.pausebutton2);
        playDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.playbutton2);
        // Get the midi port from the Application
        mApp = (FretApplication)getActivity().getApplicationContext();
        if(mOldMidi) {
            // Instantiate the driver.
            mMidiDriver = new MidiDriver();
            // Set the listener.
            mMidiDriver.setOnMidiStartListener(this);
        } else {
            setupMidi();
            mMidiDeviceInfo = mApp.getMidiDeviceInfo();
            mPortInfo = mApp.getPortInfo();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.fretview_layout, container, false);
//        mTrackName = (TextView) myView.findViewById(R.id.track_name);
        mFretTrackView = myView.findViewById(R.id.fretview);
        mFretTrackView.setFretListener(new FretTrackView.FretListener() {
            @Override
            public void OnTempoChanged(int tempo) {
                mTempo = tempo;
                mMidiPlayer.setTempo(tempo);
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
            loadTrack(mMidiReceiver, mFretSong.getTrack(mSoloTrack), mFretSong.getTpqn(), mTempo, mCurrentEvent);
//            mTrackName.setText(mFretSong.getTrackName(mSoloTrack));
        }
        return myView;
    }

    /**
     * Set the song to view - nothing shown until we supply a song to view
     *
     * @param fretSong Song to view
     */
    public void setFretSong(FretSong fretSong) {
        Log.d(TAG, "setFretSong");
        mFretSong = fretSong;
        mSoloTrack = mFretSong.getSoloTrack();
        mClickTrackSize = mFretSong.getTrack(mFretSong.getClickTrack()).getClickTrackSize();
        // Generate a list of CLick events
        mFretSong.getTrack(mSoloTrack).generateClickEventList();
        Log.d(TAG, "HELLO: ClickTrackSize"+mClickTrackSize);
        mMidiPlayer = new MidiPlayer(new MidiPlayer.OnUiUpdateRequiredListener() {
            @Override
            public void onClickEvent(int clickEvent, int currentEvent) {
                mCurrentEvent = currentEvent;
                updateProgress(mClickTrackSize, clickEvent);
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
        }, mApp.mainThreadHandler, mApp.executorService, mFretSong);
        openSynth(mMidiDeviceInfo, mPortInfo);

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if(mOldMidi){
            mMidiDriver.stop();
        }
        // TODO - The app is losing focus so need to stop the player
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        Synth.closeSynth(mOpenDevice);
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
        mMidiPlayer.setPlaying(mPlaying);
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
        int event = mFretTrack.getClickEventByClickNumber(mClickTrackSize * progress/100);
        if (event < 0) {
            event = 0;
        }
        Log.d(TAG, "HELLO move to Event: [" + event + "]");
        mCurrentEvent=event;
        mMidiPlayer.moveTo(event);
    }

    private void loadTrack(MidiReceiver midiReceiver, FretTrack frettrack, int tpqn, int tempo, int currentFretEvent) {
        Log.d(TAG, "loadTrack");
        mFretTrack = frettrack;
        mTempo = tempo;
        mFretTrackView.setFretInstrument(mFretTrack.getInstrument());
//        mFretTrackView.setNotes(mFretTrack.fretEvents.get(currentFretEvent));
        mPlaying = false;
        mFretTrackView.invalidate();   // Force redraw
        mTempoText.setText(String.valueOf(tempo));
        mMidiPlayer.setTrack(midiReceiver, frettrack, tpqn, tempo, currentFretEvent, mSoloTrack);
        getActivity().runOnUiThread(() -> playPauseButton.setVisibility(View.VISIBLE));
    }

    private void setupMidi() {
        mMidiManager = (MidiManager) getActivity().getSystemService(MIDI_SERVICE);
        if (mMidiManager == null) {
            Toast.makeText(getActivity(), "MidiManager is null!", Toast.LENGTH_LONG)
                    .show();
        }
    }

    public void openSynth(MidiDeviceInfo midiDeviceInfo, final MidiDeviceInfo.PortInfo portInfo) {
        Log.d(TAG, "HELLO openSynth");
        final MidiDeviceInfo info = midiDeviceInfo;
        mMidiManager.openDevice(info, new MidiManager.OnDeviceOpenedListener() {
            @Override
            public void onDeviceOpened(MidiDevice device) {
                if (device == null) {
                    Log.e(TAG, "HELLO could not open " + info);
                } else {
                    mOpenDevice = device;
                    MidiInputPort inputPort = mOpenDevice.openInputPort(
                            portInfo.getPortNumber());
                    if (null == inputPort) {
                        mMidiReceiver = null;
                        Toast.makeText(getActivity(), getString(R.string.synth_error)+info, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "HELLO could not open input port: " + info);
                    }else{
                        Log.d(TAG, "HELLO Port opened " + info);
                        mMidiReceiver = inputPort;
                        loadTrack(mMidiReceiver, mFretSong.getTrack(mSoloTrack), mFretSong.getTpqn(), mFretSong.getBpm(),0);
                        Log.d(TAG, "HELLO maxMessageSize=" + mMidiReceiver.getMaxMessageSize());
                    }
                }
            }
        }, null);
    }

    @Override
    public void onMidiStart() {
        Log.d(TAG, "onMidiStart()");
    }
}
