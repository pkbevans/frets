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

import java.util.ArrayList;
import java.util.List;

import static android.R.layout.simple_spinner_item;

/**
 * A placeholder fragment containing a simple view.
 */
public class FretboardFragment extends Fragment {
    private static final String TAG = "FretboardFragment";
    private FretboardView mFretboardView;
    private List<String> mTrackNames = new ArrayList<>();
    private MidiFile mMidiFile;
    private String mFileName = "/sdcard/download/escale.mid";
//    private String mFileName = "/sdcard/download/ziggy.mid";
//    private String mFileName = "/sdcard/midi/test.mid";
    private Spinner mTrackSpinner;
    ArrayAdapter<String> mTrackAdapter;
    private MidiDevice mOpenDevice;

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
        TextView fileNameText = (TextView) myView.findViewById(R.id.file_name);
        fileNameText.setText(mFileName);

        // Load up Midi file header
        LoadFileHeaderTask loadfile = new LoadFileHeaderTask(mFileName);// Do this in background
        loadfile.execute();
        Log.d(TAG, "onCreate END");
        return myView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpMidi();
    }

    void setupTrackSpinner(){
        mTrackAdapter = new ArrayAdapter<>
                (this.getActivity(), simple_spinner_item,mTrackNames);

        mTrackAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        mTrackSpinner.setAdapter(mTrackAdapter);
        // Spinner item selection Listener
        mTrackSpinner.setOnItemSelectedListener(new trackSelectedListener());
        mTrackSpinner.setEnabled(true);
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
            for(String x: mTrackNames){
                Log.d(TAG,"Trackname: "+x);
            }
            setupTrackSpinner();
        }
    }

    class trackSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            // Load up selected track
            Log.d(TAG, "Track "+pos + " selected");
            mFretboardView.loadTrack(mMidiFile, pos);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            Log.d(TAG, "onNothingSelected");
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        // The app is losing focus so need to stop the
        mFretboardView.pause();
        super.onPause();

    }

    private void setUpMidi(){
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            Log.d(TAG, "MIDI Supported");
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
                            }
                            else{
                                Log.d(TAG, "Port "+ port+ " opened.");
                                if(mFretboardView != null){
                                    mFretboardView.setInputPort(inputPort, 0);
                                }
                            }
                        }
                    }
                }, null);
            }
        } else {
            Log.d(TAG, "MIDI NOT Supported!!");
        }
    }
}
