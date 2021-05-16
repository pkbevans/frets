package com.bondevans.frets.utils;

import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;

import com.bondevans.frets.app.FretApplication;

import java.io.IOException;
import java.util.ArrayList;

public class Synth {
    public static final String TAG = Synth.class.getSimpleName();
    private static ArrayList<SynthDetails> mSynths = new ArrayList<>();

    public static class SynthDetails{
        String description;
        MidiDeviceInfo midiDeviceInfo;
        MidiDeviceInfo.PortInfo portInfo;

        public SynthDetails(String desc, MidiDeviceInfo info, MidiDeviceInfo.PortInfo portInfo) {
            description = desc;
            midiDeviceInfo = info;
            this.portInfo = portInfo;
        }
        public String getDescription(){
            return description;
        }
    }

    public static ArrayList<SynthDetails> getSynths(MidiManager midiManager){
        Log.d(TAG, "HELLO getSynths");
        MidiDeviceInfo.PortInfo[] ports;
        // Get list of connected MIdi devices
        MidiDeviceInfo[] infos = midiManager.getDevices();
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
                        mSynths.add(new SynthDetails(desc, info, portInfo));
                    }
                }
            }
        }
        return mSynths;
    }
    public static void openSynth(final FretApplication app, MidiManager midiManager, final SynthDetails synthDetails) {
        Log.d(TAG, "HELLO openSynth: "+synthDetails.toString());
        final MidiDeviceInfo info = synthDetails.midiDeviceInfo;
        midiManager.openDevice(info, new MidiManager.OnDeviceOpenedListener() {
            @Override
            public void onDeviceOpened(MidiDevice device) {
                if (device == null) {
                    Log.e(TAG, "HELLO could not open " + info);
                } else {
                    MidiDevice openDevice = device;
                    MidiInputPort inputPort = openDevice.openInputPort(
                            synthDetails.portInfo.getPortNumber());
                    if (null == inputPort) {
                        Log.e(TAG, "HELLO could not open input port on " + info);
                    }else{
                        Log.d(TAG, "HELLO Port opened " + info);
                        app.setMidiDeviceInfo(info);
                        app.setPortInfo(synthDetails.portInfo);
                        // Close
                        closeSynth(openDevice);
                    }
                }
            }
        }, null);
    }
    public static void closeSynth(MidiDevice openDevice){
        try {
            if(openDevice != null) {
                android.util.Log.d(TAG, "HELLO Closing Midi Device: "+
                        openDevice.getInfo().getProperties().getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) + " - " +
                        openDevice.getInfo().getProperties().getString(MidiDeviceInfo.PROPERTY_PRODUCT));
                openDevice.close();
            }
        } catch (IOException e) {
            android.util.Log.e (TAG, "HELLO - error closing midi device"+ e.getMessage());
        }
    }

}
