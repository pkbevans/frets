/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bondevans.frets.selectSynth;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.utils.Log;
import com.bondevans.frets.utils.Pref;

import java.io.IOException;
import java.util.ArrayList;

import static android.R.layout.simple_spinner_item;

/**
 * Main activity for the keyboard app.
 */
public class SelectSynthActivity extends Activity {
    private static final String TAG = SelectSynthActivity.class.getSimpleName();
    public static final byte STATUS_NOTE_OFF = (byte) 0x80;
    public static final byte STATUS_NOTE_ON = (byte) 0x90;

    private MidiManager mMidiManager;
    private byte[] mByteBuffer = new byte[3];
    private MidiDevice mOpenDevice;
    private MidiInputPort mInputPort;
    private MidiReceiver mReceiver;
    private ArrayAdapter synthAdapter;
    private Spinner synthSpinner;
    private ArrayList<String> mSynthDescriptions = new ArrayList<>();
    private ArrayList<SynthDetails> mSynths = new ArrayList<>();
    private FretApplication mApp;

    static class SynthDetails{
        String description;
        MidiDeviceInfo midiDeviceInfo;
        MidiDeviceInfo.PortInfo portInfo;

        public SynthDetails(String desc, MidiDeviceInfo info, MidiDeviceInfo.PortInfo portInfo) {
            description = desc;
            midiDeviceInfo = info;
            this.portInfo = portInfo;
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_synth_activity);
        mApp = (FretApplication) getApplicationContext();

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        } else {
            Toast.makeText(this, "MIDI not supported!", Toast.LENGTH_LONG)
                    .show();
        }

        synthAdapter = new ArrayAdapter<>(this, simple_spinner_item, mSynthDescriptions);
        synthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        synthSpinner = findViewById(R.id.spinner_receivers);
        synthSpinner.setAdapter(synthAdapter);
        synthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "HELLO Synth selected: "+ position + ":"+id);
                Log.d(TAG, "HELLO Synth selected: "+ mSynthDescriptions.get(position));
                if(mSynths.get(position).midiDeviceInfo != null) {
                    openSynth(mSynths.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "HELLO onNothingSelected");
            }
        });
    }

    private void setupMidi() {
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);
        if (mMidiManager == null) {
            Toast.makeText(this, "MidiManager is null!", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        getSynths();
    }

    private void getSynths(){
        Log.d(TAG, "HELLO getSynths");
        // Add dummy value at start
        MidiDeviceInfo.PortInfo[] ports;
        // Get list of connected MIdi devices
        MidiDeviceInfo[] infos = mMidiManager.getDevices();
        // Find all the Input-type ones
        for (MidiDeviceInfo info:infos) {
            Log.d(TAG, "HELLO MidiDeviceInfo");
            if(info.getInputPortCount() > 0){
                Log.d(TAG, "HELLO input port count>0");
                ports = info.getPorts();
                for (MidiDeviceInfo.PortInfo portInfo:ports) {
                    Log.d(TAG, "HELLO got portInfo");
                    if( portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT){
                        Log.d(TAG, "HELLO Got Input Port: Name: " + info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME) +
                                " Manufacturer: " + info.getProperties().getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)+
                                " Product: " + info.getProperties().getString(MidiDeviceInfo.PROPERTY_PRODUCT)+
                                " Port Number: " + portInfo.getPortNumber()+
                                " Port Name: " + portInfo.getName());

                        String desc = info.getProperties().getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)+
                                " - " + info.getProperties().getString(MidiDeviceInfo.PROPERTY_PRODUCT);
                        mSynthDescriptions.add(desc);
                        mSynths.add(new SynthDetails(desc, info, portInfo));
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeSynth();
    }

    private void closeSynth(){
        try {
            if(mOpenDevice != null) {
                Log.d(TAG, "HELLO Closing Midi Device: "+
                        mOpenDevice.getInfo().getProperties().getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) + " - " +
                        mOpenDevice.getInfo().getProperties().getString(MidiDeviceInfo.PROPERTY_PRODUCT));
                mOpenDevice.close();
            }
        } catch (IOException e) {
            Log.e (TAG, "HELLO - error closing midi device"+ e.getMessage());
        }
    }
    public void openSynth(final SynthDetails synthDetails) {
        Log.d(TAG, "HELLO openSynth: "+synthDetails.toString());
        closeSynth();   // Close anything already open first
        final MidiDeviceInfo info = synthDetails.midiDeviceInfo;
        mMidiManager.openDevice(info, new MidiManager.OnDeviceOpenedListener() {
            @Override
            public void onDeviceOpened(MidiDevice device) {
                if (device == null) {
                    Log.e(TAG, "HELLO could not open " + info);
                } else {
                    mOpenDevice = device;
                    mInputPort = mOpenDevice.openInputPort(
                            synthDetails.portInfo.getPortNumber());
                    if (null == mInputPort) {
                        Log.e(TAG, "HELLO could not open input port on " + info);
                    }else{
                        Log.d(TAG, "HELLO Port opened " + info);
                        mReceiver = mInputPort;
                        mApp.setMidiDeviceInfo(info);
                        mApp.setPortInfo(synthDetails.portInfo);
                        noteOn(0,48, 64);
                        Pref.setPreference(SelectSynthActivity.this, Pref.SYNTH, synthDetails.description);
                        finish();   // TODO - remove this
                    }
                }
            }
        }, null);
    }

    private void noteOff(int channel, int pitch, int velocity) {
        midiCommand(STATUS_NOTE_OFF + channel, pitch, velocity);
    }

    private void noteOn(int channel, int pitch, int velocity) {
        midiCommand(STATUS_NOTE_ON + channel, pitch, velocity);
    }

    private void midiCommand(int status, int data1, int data2) {
        mByteBuffer[0] = (byte) status;
        mByteBuffer[1] = (byte) data1;
        mByteBuffer[2] = (byte) data2;
        long now = System.nanoTime();
        midiSend(mByteBuffer, 3, now);
    }

    private void midiSend(byte[] buffer, int count, long timestamp) {
        Log.d(TAG, "HELLO midiSend 1");
        try {
            // send event immediately
            if (mReceiver != null) {
                Log.d(TAG, "HELLO midiSend 2");
                mReceiver.send(buffer, 0, count, timestamp);
            }
        } catch (IOException e) {
            Log.e(TAG, "HELLO midiSend failed " + e.getMessage());
        }
    }
}
