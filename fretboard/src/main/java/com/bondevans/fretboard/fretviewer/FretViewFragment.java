package com.bondevans.fretboard.fretviewer;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.fretview.FretSong;
import com.bondevans.fretboard.fretview.FretTrackView;
import com.bondevans.fretboard.utils.FileLoaderTask;

import java.io.File;
import java.io.IOException;

public class FretViewFragment extends Fragment {
    private static final String TAG = FretViewFragment.class.getSimpleName();
    private static final String KEY_MIDI = "midi";
    private boolean mMidiSupported;
    private FretTrackView mFretTrackView;
    private TextView mSongName;
    private TextView mTrackName;
    private ImageButton playPauseButton;
    private MidiDevice mOpenDevice;
    private SeekBar mSeekBar;
    private TextView mTempoText;
    private int mTempo;
    private int mCurrentEvent;
    private FretSong mFretSong;
    private ProgressDialog progressDialog;
    private Drawable playDrawable;
    private Drawable pauseDrawable;
    private boolean mPlay;

    public FretViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "onCreate");
        pauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.pausebutton2);
        playDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.playbutton2);
        if (savedInstanceState == null) {
            Log.d(TAG, "savedInstanceState == null");
            if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
                mMidiSupported = true;
                Log.d(TAG, "MIDI Supported");
            } else {
                Log.d(TAG, "MIDI NOT Supported!!");
                mMidiSupported = false;
            }
        } else {
            mMidiSupported = savedInstanceState.getBoolean(KEY_MIDI);
        }
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
    public void setFretSong(FretSong fretSong) {
        Log.d(TAG, "setFretSong");
        mFretSong = fretSong;
        mSongName.setText(mFretSong.getName());
        mTrackName.setText(mFretSong.geTrackName(mFretSong.getSoloTrack()));
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
        mFretTrackView.pause();
        progressDialog.dismiss();
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
                                if (mFretTrackView != null) {
                                    mFretTrackView.setInputPort(inputPort, 1);
                                }
                            }
                        }
                    }
                }, null);
            }
        }
    }

    public FretSong getFretSong() {
        return mFretSong;
    }
}
