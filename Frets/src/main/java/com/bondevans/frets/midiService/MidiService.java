package com.bondevans.frets.midiService;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.midi.Midi;
import com.bondevans.frets.utils.Log;

import org.billthefarmer.mididriver.MidiDriver;

import static com.bondevans.frets.midi.Midi.NOTE_OFF;

public class MidiService extends Service implements MidiDriver.OnMidiStartListener {
    private static final String TAG = MidiService.class.getSimpleName();
    private static final int SET_INSTRUMENT=0xC0;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private MidiDriver mMidiDriver;
    private byte[] midiBuffer = new byte[3];
    private byte[] midiNoteOffBuffer = new byte[3];
    private NoteOffHandler mNoteOffHandler;

    public interface OnSendMidiListener {
        void setMidiInstrument(int channel, int instrument);
        void sendMidiNotes(FretEvent fretEvent, int channel, int milliSecs);
        void allNotesOff(int channel);
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
//        mNotesOffRunnable = new NoteOffRunnable();
        mNoteOffHandler = new NoteOffHandler();
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
    public void sendMidi(byte[] buffer) {
//        Log.d(TAG, "sendMidi");
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
     * @param offMilliSecs if > zero this is the number of millisecs before a note off message is sent
     */
    public void sendMidiNotes(FretEvent fretEvent, int channel, int offMilliSecs) {
        if (fretEvent.fretNotes != null) {
            for (int i = 0; i < fretEvent.fretNotes.size(); i++) {
                // Send the MIDI notes
                sendMidiNote(fretEvent.fretNotes.get(i), channel);
                if(offMilliSecs>0) {
                    Log.d(TAG, "sleep: Note:"+ fretEvent.fretNotes.get(i).note);
                    mNoteOffHandler.sleep(offMilliSecs, fretEvent.fretNotes.get(i).note, channel);
                }
            }
        }
        if(fretEvent.bend>0){
            // Send pitch Bend message (will alter current note playing)
            midiBuffer[0] = (byte) (Midi.PITCH_WHEEL | fretEvent.track);
            midiBuffer[1] = (byte) (fretEvent.bend & 0x7F);
            midiBuffer[2] = (byte) ((byte) (fretEvent.bend >> 7) & 0x7F);
            sendMidi(midiBuffer);
        }
    }
    private void sendMidiNote(FretNote fretNote, int channel) {
        Log.d(TAG, (fretNote.on?"NOTE ON": "NOTE OFF")+ " note: "+ fretNote.note+ " to channel "+channel+"");
        midiBuffer[0] = (byte) (fretNote.on ? (Midi.NOTE_ON | channel) : (NOTE_OFF | channel));
        // Note value
        midiBuffer[1] = (byte) fretNote.note;
        // Velocity - Hardcoded volume for NOTE_ON and zero for NOTE_OFF - TODO Dont hardcode - use the volume from the midi file
        midiBuffer[2] = (byte) (fretNote.on ? 0x60 : 0x00);
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

    public void allNotesOff(int channel){
        Log.d(TAG, "Sending NEW ALL NOTES OFF");
            for (int noteValue = 0; noteValue < 128; noteValue++) {
                midiBuffer[0] = (byte) (Midi.NOTE_OFF | channel);
                // Note value
                midiBuffer[1] = (byte) noteValue;
                // Velocity - ZERO volume
                midiBuffer[2] = (byte) 0x00;
                sendMidiNoteOff(noteValue, channel);
            }
    }

    class NoteOffHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: Note:"+msg.arg1+" channel: "+msg.arg2);
            sendMidiNoteOff(msg.arg1, msg.arg2);
        }

        void sleep(long delayMillis, int note, int channel) {
            Message msg = obtainMessage();
            msg.arg1=note;
            msg.arg2=channel;
            sendMessageDelayed(msg, delayMillis);
        }
    }
}