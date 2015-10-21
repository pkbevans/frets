package com.bondevans.fretboard.fretboardplayer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.AsyncTask;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.R.layout.simple_spinner_item;

/**
 * A placeholder fragment containing a simple view.
 */
public class FretboardFragment extends Fragment {
    private static final String TAG = "FretboardFragment";
    private static final String KEY_TRACK = "track";
    private static final String KEY_FILE = "file";
    private static final String KEY_MIDI = "midi";
    private static final int NO_TRACK_SELECTED = -1;
    private boolean mMidiSupported;
    private FretboardView mFretboardView;
    private List<String> mTrackNames = new ArrayList<>();
    private MidiFile mMidiFile;
    private Spinner mTrackSpinner;
    ArrayAdapter<String> mTrackAdapter;
    TextView mFileNameText;
    private MidiDevice mOpenDevice;
    private int mSelectedTrack = NO_TRACK_SELECTED;
    private String mFileName;

    public FretboardFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "HELLO on SaveInstanceState");
        outState.putString(KEY_FILE, mFileName);
        outState.putInt(KEY_TRACK, mSelectedTrack);
        outState.putBoolean(KEY_MIDI, mMidiSupported);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if (savedInstanceState == null) {
            if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
                mMidiSupported = true;
                Log.d(TAG, "MIDI Supported");
                setUpMidi();
            } else {
                Log.d(TAG, "MIDI NOT Supported!!");
                mMidiSupported = false;
            }
        } else {
            mMidiSupported = savedInstanceState.getBoolean(KEY_MIDI);
        }
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

        return myView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            setFileName(savedInstanceState.getString(KEY_FILE));
            mSelectedTrack = savedInstanceState.getInt(KEY_TRACK);
        }
    }

    private void setupTrackSpinner() {
        mTrackAdapter = new ArrayAdapter<>
                (this.getActivity(), simple_spinner_item, mTrackNames);

        mTrackAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        mTrackSpinner.setAdapter(mTrackAdapter);
        // Spinner item selection Listener
        mTrackSpinner.setOnItemSelectedListener(new trackSelectedListener());
        mTrackSpinner.setEnabled(true);
        if (mSelectedTrack != NO_TRACK_SELECTED) {
            mFretboardView.loadTrack(mMidiFile, mSelectedTrack);
            mTrackSpinner.setSelection(mSelectedTrack);
        }
    }

    class LoadFileHeaderTask extends AsyncTask<Void, Integer, Void> {
        String midiFilePath;

        public LoadFileHeaderTask(String midiFilePath) {
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
            Log.d(TAG, "Loading header: " + midiFilePath);
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
            for (String x : mTrackNames) {
                Log.d(TAG, "Trackname: " + x);
            }
            setupTrackSpinner();
        }
    }

    class trackSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            // Load up selected track
            Log.d(TAG, "Track " + pos + " selected");
            mSelectedTrack = pos;
            mFretboardView.loadTrack(mMidiFile, pos);
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
        mFretboardView.pause();
//        closeDevice();
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
        try {
            mOpenDevice.close();
        } catch (IOException e) {
            Log.d(TAG, "ERROR: " + e.getMessage());
        } finally {
            Log.d(TAG, "FINALLY");
        }
    }


    private void setUpMidi() {
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
                            if (mFretboardView != null) {
                                mFretboardView.setInputPort(inputPort, 1);
                            }
                        }
                    }
                }
            }, null);
        }
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
        mFileNameText.setText(fileName);
        // Load up Midi file header
        LoadFileHeaderTask loadfile = new LoadFileHeaderTask(fileName);// Do this in background
        loadfile.execute();
        Log.d(TAG, "onCreate END");
    }
}
