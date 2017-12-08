package com.bondevans.frets.midiService;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.utils.Log;

import org.billthefarmer.mididriver.MidiDriver;

import static com.bondevans.frets.midi.Midi.*;

public class MidiService extends Service implements MidiDriver.OnMidiStartListener {
    private static final String TAG = MidiService.class.getSimpleName();
    private static final int SET_INSTRUMENT=0xC0;
    private static final int NO_BEND = 0;
    private static final int NO_NOTE = 0;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private MidiDriver mMidiDriver;
    private byte[] midiBuffer = new byte[3];
    private byte[] midiNoteOffBuffer = new byte[3];
    private MidiHandler mMidiHandler;

    public interface OnSendMidiListener {
        void setMidiInstrument(int channel, int instrument);
        void sendMidiNotes(FretEvent fretEvent, int channel, int milliSecs);
//        void allNotesOff(int channel);
    }

    @Override
    public void onMidiStart() {
        Log.d(TAG, "onMidiStart");
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MidiService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MidiService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");
        // Instantiate the driver.
        mMidiDriver = new MidiDriver();
        // Set the listener.
        mMidiDriver.setOnMidiStartListener(this);
        mMidiDriver.start();
        // Get the configuration.
        int[] config = mMidiDriver.config();

        // Print out the details.
        Log.d(TAG, "maxVoices: " + config[0]);
        Log.d(TAG, "numChannels: " + config[1]);
        Log.d(TAG, "sampleRate: " + config[2]);
        Log.d(TAG, "mixBufferSize: " + config[3]);
        mMidiHandler = new MidiHandler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onCreate");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mMidiDriver.stop();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnBind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind");
        super.onRebind(intent);
    }

    /** methods for clients */
    private void sendMidi(byte[] buffer) {
        mMidiDriver.write(buffer);
    }

    public void setMidiInstrument(int channel, int instrument) {
        byte[] event = new byte[2];
        event[0] = (byte) (SET_INSTRUMENT | channel);
        event[1] = (byte) instrument;
        mMidiDriver.write(event);
    }

    public void sendMidiNotes(FretEvent fretEvent, int channel) {
        sendMidiNotes(fretEvent, channel,0);
    }

    /**
     * Sends Midi notes to the synth
     * @param fretEvent FretEvent  containing the notes
     * @param channel the Midi channel
     * @param offMilli if > zero this is the number of millisecs before a note off message is sent
     */
    public void sendMidiNotes(FretEvent fretEvent, int channel, int offMilli) {
        if (fretEvent.fretNotes != null) {
            for (int i = 0; i < fretEvent.fretNotes.size(); i++) {
                // Send the MIDI notes
                mMidiHandler.send(0, fretEvent.fretNotes.get(i).on?NOTE_ON:NOTE_OFF, fretEvent.fretNotes.get(i).note, channel, NO_BEND);
                if(offMilli>0) {
                    // Queue up a NOTE_OFF
                    mMidiHandler.send(offMilli, NOTE_OFF, fretEvent.fretNotes.get(i).note, channel, NO_BEND);
                }
            }
        }
        if(fretEvent.bend>0){
            // Send pitch Bend message (will alter current note playing)
            mMidiHandler.send(offMilli, PITCH_WHEEL, NO_NOTE, channel, fretEvent.bend);
        }
    }
    private void sendMidiNote(int what, int note, int channel) {
//        Log.d(TAG, (what == NOTE_ON?"NOTE ON": "NOTE OFF")+ " note: "+ note+ " to channel "+channel+"");
        midiBuffer[0] = (byte) (what| channel);
        // Note value
        midiBuffer[1] = (byte) note;
        // Velocity - Hardcoded volume for NOTE_ON and zero for NOTE_OFF
        midiBuffer[2] = (byte) (what == NOTE_ON? 0x60 : 0x00);
        sendMidi(midiBuffer);
    }
    private void sendMidiNoteOff(int note, int channel){
        midiNoteOffBuffer[0] = (byte) (NOTE_OFF | channel);
        // Note value
        midiNoteOffBuffer[1] = (byte) note;
        // Velocity - zero for NOTE_OFF
        midiNoteOffBuffer[2] = (byte) 0x00;
        sendMidi(midiNoteOffBuffer);
    }

    private void sendMidiBend(int bend, int channel){
        midiBuffer[0] = (byte) (PITCH_WHEEL | channel);
        midiBuffer[1] = (byte) (bend & 0x7F);
        midiBuffer[2] = (byte) ((byte) (bend >> 7) & 0x7F);
        sendMidi(midiBuffer);
    }

    public void allNotesOff(int channel){
        Log.d(TAG, "Sending NEW ALL NOTES OFF");
            for (int noteValue = 0; noteValue < 128; noteValue++) {
                midiBuffer[0] = (byte) (NOTE_OFF | channel);
                // Note value
                midiBuffer[1] = (byte) noteValue;
                // Velocity - ZERO volume
                midiBuffer[2] = (byte) 0x00;
                sendMidiNoteOff(noteValue, channel);
            }
    }

    class MidiHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
//            Log.d(TAG, "handleMessage: Note:"+msg.arg1+" channel: "+msg.arg2);
            if(msg.what==PITCH_WHEEL){
                sendMidiBend(msg.arg1, msg.arg2);
            }
            else {
                sendMidiNote(msg.what, msg.arg1, msg.arg2);
            }
        }

        void send(long delayMillis, int what, int note, int channel, int bend) {
            Message msg = obtainMessage();
            msg.what=what;
            if(what==PITCH_WHEEL){
                msg.arg1 = bend;
            }
            else {
                msg.arg1 = note;
            }
            msg.arg2 = channel;
            sendMessageDelayed(msg, delayMillis);
        }
    }
}