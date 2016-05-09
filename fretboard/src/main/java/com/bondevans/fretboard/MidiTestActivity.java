package com.bondevans.fretboard;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import org.billthefarmer.mididriver.MidiDriver;

public class MidiTestActivity extends AppCompatActivity implements MidiDriver.OnMidiStartListener,
        View.OnTouchListener {

    private static final String TAG = MidiTestActivity.class.getSimpleName();
    private MidiDriver mMidiDriver;
    private byte[] mEvent;
    private int[] mConfig;
    private Button buttonPlayNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.miditest_activity);

        buttonPlayNote = (Button) findViewById(R.id.buttonPlayNote);
        buttonPlayNote.setOnTouchListener(this);

        // Instantiate the driver.
        mMidiDriver = new MidiDriver();
        // Set the listener.
        mMidiDriver.setOnMidiStartListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMidiDriver.start();

        // Get the configuration.
        mConfig = mMidiDriver.config();

        // Print out the details.
        Log.d(this.getClass().getName(), "maxVoices: " + mConfig[0]);
        Log.d(this.getClass().getName(), "numChannels: " + mConfig[1]);
        Log.d(this.getClass().getName(), "sampleRate: " + mConfig[2]);
        Log.d(this.getClass().getName(), "mixBufferSize: " + mConfig[3]);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMidiDriver.stop();
    }

    @Override
    public void onMidiStart() {
        Log.d(this.getClass().getName(), "onMidiStart()");
    }

    private void playNote() {

        // Construct a note ON message for the middle C at maximum velocity on channel 1:
        mEvent = new byte[3];
        mEvent[0] = (byte) (0x90 | 0x00);  // 0x90 = note On, 0x00 = channel 1
        mEvent[1] = (byte) 0x3C;  // 0x3C = middle C
        mEvent[2] = (byte) 0x7F;  // 0x7F = the maximum velocity (127)

        // Internally this just calls write() and can be considered obsoleted:
        //mMidiDriver.queueEvent(mEvent);

        // Send the MIDI mEvent to the synthesizer.
        mMidiDriver.write(mEvent);
    }

    private void stopNote() {

        // Construct a note OFF message for the middle C at minimum velocity on channel 1:
        mEvent = new byte[3];
        mEvent[0] = (byte) (0x80 | 0x00);  // 0x80 = note Off, 0x00 = channel 1
        mEvent[1] = (byte) 0x3C;  // 0x3C = middle C
        mEvent[2] = (byte) 0x00;  // 0x00 = the minimum velocity (0)

        // Send the MIDI mEvent to the synthesizer.
        mMidiDriver.write(mEvent);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        Log.d(this.getClass().getName(), "Motion mEvent: " + event);

        if (v.getId() == R.id.buttonPlayNote) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Log.d(this.getClass().getName(), "MotionEvent.ACTION_DOWN");
                playNote();
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Log.d(this.getClass().getName(), "MotionEvent.ACTION_UP");
                stopNote();
            }
        }

        return false;
    }
}