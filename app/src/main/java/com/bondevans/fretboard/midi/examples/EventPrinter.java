package com.bondevans.fretboard.midi.examples;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import com.bondevans.fretboard.midi.MidiFile;
import com.bondevans.fretboard.midi.event.MidiEvent;
import com.bondevans.fretboard.midi.event.NoteOn;
import com.bondevans.fretboard.midi.event.meta.Tempo;
import com.bondevans.fretboard.midi.util.MidiEventListener;
import com.bondevans.fretboard.midi.util.MidiProcessor;

public class EventPrinter implements MidiEventListener
{
    private static final String TAG = "EventPrinter";
    private String mLabel;

    public EventPrinter(String label)
    {
        mLabel = label;
    }

    // 0. Implement the listener functions that will be called by the
    // MidiProcessor
    @Override
    public void onStart(boolean fromBeginning)
    {
        if(fromBeginning)
        {
            Log.d(TAG, mLabel + " Started!");
        }
        else
        {
            Log.d(TAG, mLabel + " resumed");
        }
    }

    @Override
    public void onEvent(MidiEvent event, long ms)
    {
        Log.d(TAG, mLabel + " received event: " + event);
    }

    @Override
    public void onStop(boolean finished)
    {
        if(finished)
        {
            Log.d(TAG, mLabel + " Finished!");
        }
        else
        {
            Log.d(TAG, mLabel + " paused");
        }
    }

    public static void main(String[] args)
    {
        // 1. Read in a MidiFile
        MidiFile midi = null;
        try
        {
            midi = new MidiFile(new File("inputmid.mid"));
        }
        catch(IOException e)
        {
            System.err.println(e);
            return;
        }

        // 2. Create a MidiProcessor
        MidiProcessor processor = new MidiProcessor(midi);

        // 3. Register listeners for the events you're interested in
        EventPrinter ep = new EventPrinter("Individual Listener");
        processor.registerEventListener(ep, Tempo.class);
        processor.registerEventListener(ep, NoteOn.class);

        // or listen for all events:
        EventPrinter ep2 = new EventPrinter("Listener For All");
        processor.registerEventListener(ep2, MidiEvent.class);

        // 4. Start the processor
        processor.start();

        // Listeners will be triggered in real time with the MIDI events
        // And you can pause/resume with stop() and start()
        try
        {
            Thread.sleep(10 * 1000);
            processor.stop();

            Thread.sleep(10 * 1000);
            processor.start();
        }
        catch(Exception e)
        {
        }
    }
}
