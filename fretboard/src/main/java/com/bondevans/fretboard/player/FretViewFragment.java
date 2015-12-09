package com.bondevans.fretboard.player;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.fretview.FretSong;
import com.bondevans.fretboard.fretview.FretView;
import com.bondevans.fretboard.fretview.FretSongLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.R.layout.simple_spinner_item;

/**
 * A placeholder fragment containing a simple view.
 */
public class FretViewFragment extends Fragment {
    private static final String TAG = FretViewFragment.class.getSimpleName();
    private static final String KEY_TRACK = "track";
    private static final String KEY_FILE = "file";
    private static final String KEY_MIDI = "midi";
    private static final int NO_TRACK_SELECTED = -1;
    private static final String KEY_TEMPO = "instrument";
    private static final String KEY_PROGRESS = "progress";
    private boolean mMidiSupported;
    private FretView mFretView;
    private List<SongTrack> mTracks = new ArrayList<>();
    private Spinner mTrackSpinner;
    ArrayAdapter<SongTrack> mTrackAdapter;
    TextView mSongName;
    private ImageButton playPauseButton;
    private MidiDevice mOpenDevice;
    private int mSelectedTrack = NO_TRACK_SELECTED;
    private String mFileName;
    private SeekBar mSeekBar;
    private TextView mTempoText;
    private int mTempo;
    private int mProgress;
    private FretSong mFretSong;
    private ProgressDialog progressDialog;
    private Drawable playDrawable;
    private Drawable pauseDrawable;
    private boolean mPlay;

    public FretViewFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "HELLO on SaveInstanceState-");
        outState.putString(KEY_FILE, mFileName);
        outState.putInt(KEY_TRACK, mSelectedTrack);
        outState.putBoolean(KEY_MIDI, mMidiSupported);
        outState.putInt(KEY_TEMPO, mTempo);
        outState.putInt(KEY_PROGRESS, mProgress);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        pauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.pausebutton2);
        playDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.playbutton2);
        if (savedInstanceState == null) {
            if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
                mMidiSupported = true;
                Log.d(TAG, "MIDI Supported");
//                setUpMidi();
            } else {
                Log.d(TAG, "MIDI NOT Supported!!");
                mMidiSupported = false;
            }
        } else {
            mMidiSupported = savedInstanceState.getBoolean(KEY_MIDI);
        }
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.fretview_layout, container, false);
        mFretView = (FretView) myView.findViewById(R.id.fretboard);
        mFretView.setFretListener(new FretView.FretListener() {
            @Override
            public void OnProgressUpdated(int progress) {
                mProgress = progress;
                mSeekBar.setProgress(progress);
            }

            @Override
            public void OnTempoChanged(int tempo) {
                mTempo = tempo;
                mTempoText.setText("" + tempo);
            }

            @Override
            public void OnPlayEnabled(boolean flag) {
                mPlay = true;
                progressDialog.hide();
            }
        });
        mTempoText = (TextView) myView.findViewById(R.id.bpmText);
        mTrackSpinner = (Spinner) myView.findViewById(R.id.track_spinner);
        mTrackSpinner.setEnabled(false);
        mSongName = (TextView) myView.findViewById(R.id.file_name);
        mSeekBar = (SeekBar) myView.findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                Log.d(TAG, "onProgressChanged [" + progress + "] " + (fromUser ? "FROM USER" : "" + ""));
                // If user moves the position, then need to update current fretEvent in Fretboard view
                // progress = a value between 0-100 - e.g. percent of way through the song
                if (fromUser) {
                    mFretView.moveTo(progress);
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
                mFretView.play();
            }
        });
        return myView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            setFileName(savedInstanceState.getString(KEY_FILE));
            mSelectedTrack = savedInstanceState.getInt(KEY_TRACK);
            mTempo = savedInstanceState.getInt(KEY_TEMPO);
            mProgress = savedInstanceState.getInt(KEY_PROGRESS);
        }
    }

    private void setupTrackSpinner() {
        mTrackAdapter = new ArrayAdapter<>
                (this.getActivity(), simple_spinner_item, mTracks);

        mTrackAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        mTrackSpinner.setAdapter(mTrackAdapter);
        // Spinner item selection Listener
        mTrackSpinner.setOnItemSelectedListener(new trackSelectedListener());
        mTrackSpinner.setEnabled(true);
    }

    public void export() {
        if( mSelectedTrack== NO_TRACK_SELECTED){
            Toast.makeText(getActivity(), R.string.no_track, Toast.LENGTH_SHORT).show();
            return;
        }
        String xx = mFretSong.toString();
        Log.d(TAG, "HELLO - export: ["+xx+"]");
    }

    class trackSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            // Load up selected track
            SongTrack track = (SongTrack) parent.getItemAtPosition(pos);
            if(mSelectedTrack == pos) {
                Log.d(TAG, "ORIENTATION CHANGE Track " + pos + " selected: "+ track);
                // must be orientation change
                mFretView.setTrack(mFretSong.getTrack(track.index), mFretSong.getTpqn(), mTempo, mProgress);
            }
            else{
                // Must be different track selected in current session
                mFretView.setTrack(mFretSong.getTrack(track.index), mFretSong.getTpqn(), mFretSong.getBpm());
            }
            mSelectedTrack = pos;
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            Log.d(TAG, "onNothingSelected");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // The app is losing focus so need to stop the
        mFretView.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if(mMidiSupported){
            setUpMidi();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        closeDevice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    void closeDevice() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            Log.d(TAG, "closeDevice");

            if (mOpenDevice != null) {
                try {
                    mOpenDevice.close();
                } catch (IOException e) {
                    Log.d(TAG, "ERROR: " + e.getMessage());
                } finally {
                    Log.d(TAG, "FINALLY");
                }
            }
        }
    }

    private void setUpMidi() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {

            Log.d(TAG, "Setting up MIDI");
            MidiManager m = (MidiManager) getActivity().getSystemService(Context.MIDI_SERVICE);
            final MidiDeviceInfo[] infos = m.getDevices();
            if (infos == null || infos.length == 0) {
                Log.d(TAG, "NO MIDI DEVICES");
            } else {
                for (MidiDeviceInfo d : infos) {
                    Bundle properties = d.getProperties();
                    Log.d(TAG, "MIDI DEVICE NAME: " + properties.getString(MidiDeviceInfo.PROPERTY_NAME));
                    Log.d(TAG, "MIDI DEVICE MANUFACTURER: " + properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER));
                    Log.d(TAG, "MIDI DEVICE PRODUCT: " + properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT));
                    Log.d(TAG, "MIDI DEVICE SERIAL_NUMBER: " + properties.getString(MidiDeviceInfo.PROPERTY_SERIAL_NUMBER));
                    Log.d(TAG, "MIDI DEVICE USB_DEVICE: " + properties.getString(MidiDeviceInfo.PROPERTY_USB_DEVICE));
                    Log.d(TAG, "MIDI DEVICE BLUETOOTH_EDVICE: " + properties.getString(MidiDeviceInfo.PROPERTY_BLUETOOTH_DEVICE));
                    Log.d(TAG, "MIDI DEVICE VERSION: " + properties.getString(MidiDeviceInfo.PROPERTY_VERSION));
                }
                final MidiDeviceInfo info = infos[0];
                // Lets try to open the first device
                m.openDevice(info, new MidiManager.OnDeviceOpenedListener() {

                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDeviceOpened(MidiDevice device) {
                        if (device == null) {
                            Log.e(TAG, "could not open device " + info);
                        } else {
                            Log.d(TAG, "Device opened");
                            mOpenDevice = device;
                            // TODO - what port to open???
                            int port = 0;
                            MidiInputPort inputPort = mOpenDevice.openInputPort(port);
                            if (inputPort == null) {
                                Log.e(TAG, "could not open input port on " + info);
                            } else {
                                Log.d(TAG, "Port " + port + " opened.");
                                if (mFretView != null) {
                                    mFretView.setInputPort(inputPort, 1);
                                }
                            }
                        }
                    }
                }, null);
            }
        }
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
        FretSongLoader fretSongLoader = new FretSongLoader(new File(fileName));
        fretSongLoader.setSongLoadedListener(new FretSongLoader.SongLoadedListener() {
            @Override
            public void OnSongLoaded(FretSong song) {
                mFretSong = song;
                mSongName.setText(mFretSong.getName());
                mTracks = mFretSong.getTrackNames();
                setupTrackSpinner();
            }

            @Override
            public void OnError(String msg) {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        });
        fretSongLoader.execute();
    }
}
