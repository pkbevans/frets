package com.bondevans.frets.fretlist;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.bondevans.frets.R;

import org.billthefarmer.mididriver.MidiDriver;

public class MidiTest extends AppCompatActivity implements MidiDriver.OnMidiStartListener,
        View.OnTouchListener, AdapterView.OnItemSelectedListener {

    private MidiDriver midiDriver;
    private byte[] event;
    private int[] config;

    private Button buttonSustain;

    private Button buttonC;
    private Button buttonCsharp;
    private Button buttonD;
    private Button buttonDsharp;
    private Button buttonE;
    private Button buttonF;
    private Button buttonFsharp;
    private Button buttonG;
    private Button buttonGsharp;
    private Button buttonA;
    private Button buttonAsharp;
    private Button buttonB;
    private Button buttonC2;

    private Spinner spinnerInstruments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.miditest_activity);

        // Set up the buttons.
        buttonSustain = (Button)findViewById(R.id.buttonSustain);
        buttonSustain.setOnTouchListener(this);

        buttonC = (Button)findViewById(R.id.buttonC);
        buttonC.setOnTouchListener(this);
        buttonCsharp = (Button)findViewById(R.id.buttonCsharp);
        buttonCsharp.setOnTouchListener(this);
        buttonD = (Button)findViewById(R.id.buttonD);
        buttonD.setOnTouchListener(this);
        buttonDsharp = (Button)findViewById(R.id.buttonDsharp);
        buttonDsharp.setOnTouchListener(this);
        buttonE = (Button)findViewById(R.id.buttonE);
        buttonE.setOnTouchListener(this);
        buttonF = (Button)findViewById(R.id.buttonF);
        buttonF.setOnTouchListener(this);
        buttonFsharp = (Button)findViewById(R.id.buttonFsharp);
        buttonFsharp.setOnTouchListener(this);
        buttonG = (Button)findViewById(R.id.buttonG);
        buttonG.setOnTouchListener(this);
        buttonGsharp = (Button)findViewById(R.id.buttonGsharp);
        buttonGsharp.setOnTouchListener(this);
        buttonA = (Button)findViewById(R.id.buttonA);
        buttonA.setOnTouchListener(this);
        buttonAsharp = (Button)findViewById(R.id.buttonAsharp);
        buttonAsharp.setOnTouchListener(this);
        buttonB = (Button)findViewById(R.id.buttonB);
        buttonB.setOnTouchListener(this);
        buttonC2 = (Button)findViewById(R.id.buttonC2);
        buttonC2.setOnTouchListener(this);


        // Set up the instruments spinner.
        spinnerInstruments = (Spinner)findViewById(R.id.spinnerInstruments);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.midi_instrument_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInstruments.setAdapter(adapter);
        spinnerInstruments.setOnItemSelectedListener(this);

        // Instantiate the driver.
        midiDriver = new MidiDriver();
        // Set the listener.
        midiDriver.setOnMidiStartListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        midiDriver.start();

        // Get the configuration.
        config = midiDriver.config();

        // Print out the details.
        Log.d(this.getClass().getName(), "maxVoices: " + config[0]);
        Log.d(this.getClass().getName(), "numChannels: " + config[1]);
        Log.d(this.getClass().getName(), "sampleRate: " + config[2]);
        Log.d(this.getClass().getName(), "mixBufferSize: " + config[3]);


    }

    @Override
    protected void onPause() {
        super.onPause();
        midiDriver.stop();
    }

    @Override
    public void onMidiStart() {
        Log.d(this.getClass().getName(), "onMidiStart()");
    }

    private void playNote(int noteNumber) {

        // Construct a note ON message for the note at maximum velocity on channel 1:
        event = new byte[3];
        event[0] = (byte) (0x90 | 0x00);  // 0x90 = note On, 0x00 = channel 1
        event[1] = (byte) noteNumber;
        event[2] = (byte) 0x7F;  // 0x7F = the maximum velocity (127)

        // Send the MIDI event to the synthesizer.
        midiDriver.write(event);

    }

    private void stopNote(int noteNumber, boolean sustainUpEvent) {

        // Stop the note unless the sustain button is currently pressed. Or stop the note if the
        // sustain button was depressed and the note's button is not pressed.
        if (!buttonSustain.isPressed() || sustainUpEvent) {
            // Construct a note OFF message for the note at minimum velocity on channel 1:
            event = new byte[3];
            event[0] = (byte) (0x80 | 0x00);  // 0x80 = note Off, 0x00 = channel 1
            event[1] = (byte) noteNumber;
            event[2] = (byte) 0x00;  // 0x00 = the minimum velocity (0)

            // Send the MIDI event to the synthesizer.
            midiDriver.write(event);
        }
    }

    private void selectInstrument(int instrument) {

        // Construct a program change to select the instrument on channel 1:
        event = new byte[2];
        event[0] = (byte)(0xC0 | 0x00); // 0xC0 = program change, 0x00 = channel 1
        event[1] = (byte)instrument;

        // Send the MIDI event to the synthesizer.
        midiDriver.write(event);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        Log.d(this.getClass().getName(), "Motion event: " + event);

        if (v.getId() == R.id.buttonSustain && event.getAction() == MotionEvent.ACTION_UP) {
            // Stop any notes whose buttons are not held down.
            if (!buttonC.isPressed()) {
                stopNote(60, true);
            }
            if (!buttonCsharp.isPressed()) {
                stopNote(61, true);
            }
            if (!buttonD.isPressed()) {
                stopNote(62, true);
            }
            if (!buttonDsharp.isPressed()) {
                stopNote(63, true);
            }
            if (!buttonE.isPressed()) {
                stopNote(64, true);
            }
            if (!buttonF.isPressed()) {
                stopNote(65, true);
            }
            if (!buttonFsharp.isPressed()) {
                stopNote(66, true);
            }if (!buttonG.isPressed()) {
                stopNote(67, true);
            }
            if (!buttonGsharp.isPressed()) {
                stopNote(68, true);
            }
            if (!buttonA.isPressed()) {
                stopNote(69, true);
            }
            if (!buttonAsharp.isPressed()) {
                stopNote(70, true);
            }
            if (!buttonB.isPressed()) {
                stopNote(71, true);
            }
            if (!buttonC2.isPressed()) {
                stopNote(72, true);
            }
        }

        int noteNumber;

        switch (v.getId()) {
            case R.id.buttonC:
                noteNumber = 60;
                break;
            case R.id.buttonCsharp:
                noteNumber = 61;
                break;
            case R.id.buttonD:
                noteNumber = 62;
                break;
            case R.id.buttonDsharp:
                noteNumber = 63;
                break;
            case R.id.buttonE:
                noteNumber = 64;
                break;
            case R.id.buttonF:
                noteNumber = 65;
                break;
            case R.id.buttonFsharp:
                noteNumber = 66;
                break;
            case R.id.buttonG:
                noteNumber = 67;
                break;
            case R.id.buttonGsharp:
                noteNumber = 68;
                break;
            case R.id.buttonA:
                noteNumber = 69;
                break;
            case R.id.buttonAsharp:
                noteNumber = 70;
                break;
            case R.id.buttonB:
                noteNumber = 71;
                break;
            case R.id.buttonC2:
                noteNumber = 72;
                break;
            default:
                noteNumber = -1;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(this.getClass().getName(), "MotionEvent.ACTION_DOWN");
            playNote(noteNumber);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Log.d(this.getClass().getName(), "MotionEvent.ACTION_UP");
            stopNote(noteNumber, false);
        }

        return false;
    }

    /**
     * <p>Callback method to be invoked when an item in this view has been
     * selected. This callback is invoked only when the newly selected
     * position is different from the previously selected position or if
     * there was no selected item.</p>
     * <p/>
     * Impelmenters can call getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param parent   The AdapterView where the selection happened
     * @param view     The view within the AdapterView that was clicked
     * @param position The position of the view in the adapter
     * @param id       The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectInstrument(position);
    }

    /**
     * Callback method to be invoked when the selection disappears from this
     * view. The selection can disappear for instance when touch is activated
     * or when the adapter becomes empty.
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
