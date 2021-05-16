package com.bondevans.frets.fretviewer;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bondevans.frets.R;
import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.fretview.FretTrackView;
import com.bondevans.frets.midi.Midi;

import org.billthefarmer.mididriver.MidiDriver;

public class FretViewFragment extends Fragment implements MidiDriver.OnMidiStartListener {
    private static final String TAG = FretViewFragmentNew.class.getSimpleName();
    private FretTrackView mFretTrackView;
//    private TextView mTrackName;
    private ImageButton playPauseButton;
    private SeekBar mSeekBar;
    private TextView mTempoText;
    private int mTempo;
    private int mCurrentEvent;
    private FretSong mFretSong;
    private Drawable playDrawable;
    private Drawable pauseDrawable;
    private boolean mPlaying;
    private MidiDriver mMidiDriver;
    private FretEventHandler mFretEventHandler;
    private int mCurrentFretEvent = 0;
    private byte[] midiBuffer = new byte[3];
    private static final int MIDI_CHANNEL_DRUMS = 9;
    private FretTrack mFretTrack;
    private int mTicksPerQtrNote;
    private int mSoloTrack;
    private int mClickTrackSize;

    public FretViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "onCreate");
        pauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.pausebutton2);
        playDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.playbutton2);
        // Instantiate the driver.
        mMidiDriver = new MidiDriver();
        // Set the listener.
        mMidiDriver.setOnMidiStartListener(this);
        mFretEventHandler = new FretEventHandler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View myView = inflater.inflate(R.layout.fretview_layout, container, false);
//        mTrackName = (TextView) myView.findViewById(R.id.track_name);
        mFretTrackView = myView.findViewById(R.id.fretview);
        mFretTrackView.setFretListener(new FretTrackView.FretListener() {
            @Override
            public void OnTempoChanged(int tempo) {
                mTempo = tempo;
                mTempoText.setText(String.valueOf(tempo));
            }
        });
        mFretTrackView.setKeepScreenOn(true);

        mTempoText = myView.findViewById(R.id.bpmText);
        mSeekBar = myView.findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // If user moves the position, then need to update current fretEvent in Fretboard view
                // progress = a value between 0-100 - e.g. percent of way through the song
                if (fromUser) {
                    moveTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        playPauseButton = myView.findViewById(R.id.playPauseButton);
        playPauseButton.setVisibility(View.INVISIBLE);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(); // Toggle Play/pause
            }
        });
        if (savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState != null");
            // Must be orientation change
            setTrack(mFretSong.getTrack(mSoloTrack), mFretSong.getTpqn(), mTempo, mCurrentEvent);
//            mTrackName.setText(mFretSong.getTrackName(mSoloTrack));
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
        mSoloTrack = mFretSong.getSoloTrack();
        mClickTrackSize = mFretSong.getTrack(mFretSong.getClickTrack()).getClickTrackSize();
        // Generate a list of CLick events
        mFretSong.getTrack(mSoloTrack).generateClickEventList();
        Log.d(TAG, "HELLO: ClickTrackSize"+mClickTrackSize);
//        mTrackName.setText(mFretSong.getTrackName(mSoloTrack));
        setTrack(mFretSong.getTrack(mSoloTrack), mFretSong.getTpqn(), mFretSong.getBpm(),0);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // The app is losing focus so need to stop the player
        mMidiDriver.stop();
        if(mPlaying) {
            // Playing so pause
            play();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mMidiDriver.start();
        // Get the configuration.
        int[] config = mMidiDriver.config();

        // Print out the details.
        Log.d(TAG, "maxVoices: " + config[0]);
        Log.d(TAG, "numChannels: " + config[1]);
        Log.d(TAG, "sampleRate: " + config[2]);
        Log.d(TAG, "mixBufferSize: " + config[3]);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onMidiStart() {
        Log.d(TAG, "onMidiStart()");
    }

    private void setMidiInstruments(){
        Log.d(TAG, "setMidiInstruments");
        for(int track=0; track<mFretSong.tracks();track++){
            FretTrack fretTrack = mFretSong.getTrack(track);
            if(!fretTrack.isDrumTrack() && !fretTrack.isClickTrack()) {
                // No instrument for drum track - just channel 10.  No instrument for click track
                setMidiInstrument( track, fretTrack.getMidiInstrument());
            }
        }
    }
    private void setMidiInstrument(int channel, int instrument) {
        byte[] event = new byte[2];
        event[0] = (byte) (Midi.SET_INSTRUMENT | channel);
        event[1] = (byte) instrument;
        mMidiDriver.write(event);
    }

    public FretSong getFretSong() {
        return mFretSong;
    }

    class FretEventHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
//            Log.d(TAG, "handleMessage fretEvent[" + mCurrentFretEvent + "]");
            if (mPlaying) {
                // Handle event
                handleEvent();
            }
        }

        void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }
    private void handleEvent() {
        // Get current time
        long begin = System.currentTimeMillis();
        // Send next set of notes to Fretboard View
//        Log.d(TAG, "current event: "+mCurrentEvent);
        boolean redraw=false;
        do {
            FretEvent fretEvent = mFretTrack.fretEvents.get(mCurrentFretEvent);
            Log.d(TAG, "HELLO EVENT:[" + fretEvent.track + "]["+ fretEvent.getTicks()+"]["+fretEvent.totalTicks+"]["+fretEvent.bend+"]["+(fretEvent.bend & 0x7F)+"]["+((fretEvent.bend >> 7) & 0x7F)+"]");
            if (fretEvent.tempo > 0) {
                mTempo = fretEvent.tempo;
            } else if(fretEvent.isClickEvent()) {
                // Update progress listener (so it can update the seekbar (or whatever)
                Log.d(TAG, "HELLO - updateProgress: "+ fretEvent.getClickEvent() + ":" + fretEvent.totalTicks);
                updateProgress(mClickTrackSize, fretEvent.getClickEvent());
            } else {
                sendMidiNotes(fretEvent);
                if (fretEvent.track == mSoloTrack) {
                    mFretTrackView.setNotes(fretEvent.fretNotes, 0);
                    // Force redraw
                    redraw = true;
                }
            }

            // Loop round to start
            if (++mCurrentFretEvent >= mFretTrack.fretEvents.size()) {
                mCurrentFretEvent = 0;
            }
        } while (mFretTrack.fretEvents.get(mCurrentFretEvent).deltaTicks==0);
//        Log.d(TAG, "sleeping: "+mFretTrack.fretEvents.get(mCurrentFretEvent).deltaTicks+" milisecs:"+delayFromTicks(mFretTrack.fretEvents.get(mCurrentFretEvent).deltaTicks));

        mFretEventHandler.sleep(delayFromTicks(begin, mFretTrack.fretEvents.get(mCurrentFretEvent).deltaTicks));
        if(redraw){
            mFretTrackView.invalidate();
        }
    }
    /**
     * Plays/pauses the current track
     */
    public void play() {
        // toggle play/pause
        mPlaying = !mPlaying;
        playPauseButton.setImageDrawable(mPlaying ? pauseDrawable: playDrawable );
        if (mPlaying) {
            // Play
            setMidiInstruments();
            mFretEventHandler.sleep(mFretTrack.fretEvents.get(mCurrentFretEvent).deltaTicks);
        } else {
            // Pause
            sendMidiNotesOff();
        }
    }


    private void sendMidiNotes(FretEvent fretEvent) {
        if (fretEvent.fretNotes != null) {
            for (int i = 0; i < fretEvent.fretNotes.size(); i++) {
                // Send the MIDI notes
                // Is it a drum track? - Channel 10, else use track position
//                Log.d(TAG, "Ticks:"+fretEvent.getTicks()+" Channel:"+fretEvent.track);
                sendMidiNote(fretEvent.fretNotes.get(i), mFretSong.getTrack(fretEvent.track).isDrumTrack()?MIDI_CHANNEL_DRUMS:fretEvent.track);
            }
        }
        if(fretEvent.bend>0){
            // Send pitch Bend message (will alter current note playing)
            midiBuffer[0] = (byte) (Midi.PITCH_WHEEL | fretEvent.track);
            midiBuffer[1] = (byte) (fretEvent.bend & 0x7F);
            midiBuffer[2] = (byte) ((byte) (fretEvent.bend >> 7) & 0x7F);
//            Log.d(TAG, "HELLO BEND:["+ fretEvent.getTicks()+"]["+fretEvent.totalTicks+"]["+fretEvent.bend+"]["+(fretEvent.bend & 0x7F)+"]["+((fretEvent.bend >> 7) & 0x7F)+"]");
            sendMidi(midiBuffer);
        }
    }

    private void sendMidiNote(FretNote fretNote, int channel) {
//        Log.d(TAG, (fretNote.on?"NOTE ON": "NOTE OFF")+ " note: "+ fretNote.note+ " to channel "+channel+"");
        midiBuffer[0] = (byte) (fretNote.on ? (Midi.NOTE_ON | channel) : (Midi.NOTE_OFF | channel));
        // Note value
        midiBuffer[1] = (byte) fretNote.note;
        // Velocity - Hardcoded volume for NOTE_ON and zero for NOTE_OFF - TODO Dont hardcode - use the volume from the midi file
        midiBuffer[2] = (byte) (fretNote.on ? 0x60 : 0x00);
        sendMidi(midiBuffer);
    }

    /**
     * Sends NOTE_OFF message for all 128 notes on all channels used.
     */
    private void sendMidiNotesOff() {
        Log.d(TAG, "Sending NEW ALL NOTES OFF");
        for(int channel=0;channel<mFretSong.tracks();channel++) {
            for (int noteValue = 0; noteValue < 128; noteValue++) {
                midiBuffer[0] = (byte) (Midi.NOTE_OFF | (mFretSong.getTrack(channel).isDrumTrack()?MIDI_CHANNEL_DRUMS:channel));
                // Note value
                midiBuffer[1] = (byte) noteValue;
                // Velocity - ZERO volume
                midiBuffer[2] = (byte) 0x00;
                sendMidi(midiBuffer);
            }
        }
    }

    private void sendMidi(byte[] buffer) {
        mMidiDriver.write(buffer);
    }

    public void updateProgress(int length, int current) {
        mCurrentEvent = current;
        mSeekBar.setProgress((current * 100 / length)+1 );
    }

    /**
     * Moves to new position in the midi event list - in response to user moving the seek bar
     *
     * @param progress - percentage through the song
     *
     */
    private void moveTo(int progress) {
        mCurrentFretEvent = mFretTrack.getClickEventByClickNumber(mClickTrackSize * progress/100);
        if (mCurrentFretEvent < 0) {
            mCurrentFretEvent = 0;
        }
        Log.d(TAG, "mCurrentFretEvent [" + mCurrentFretEvent + "]");
    }

    /**
     * Calculates the time between FretEvents from the delta time (clicks) and the mTicks per beat setting
     * in the file
     *
     * @param deltaTicks The delta time in ticks for this event
     * @return Returns the actual delay in millisecs for this event
     */
    private long delayFromTicks(long begin, int deltaTicks) {
        long ret = 0;
        long end = System.currentTimeMillis();

        if (mTicksPerQtrNote > 0 && deltaTicks > 0) {
            // Avoid divide by zero error
            double x = ((60 * 1000) / mTempo);
            double y = x / mTicksPerQtrNote;
            double z = deltaTicks * y;
            ret = (long) z;
        }

        Log.d(TAG, "HELLO delayFromTicks ["+(end-begin) +"][" + deltaTicks+"]["+mTempo+"][" +mTicksPerQtrNote+"]["+ret+"]");
        ret = end - begin > ret? 0 : ret - (end - begin) ;
        return ret;
    }

    private void setTrack(FretTrack frettrack, int tpqn, int tempo, int currentFretEvent) {
        Log.d(TAG, "setTrack");
        mFretTrack = frettrack;
        mTempo = tempo;
        mCurrentFretEvent = currentFretEvent;
        mFretTrackView.setFretInstrument(mFretTrack.getInstrument());
        mFretTrackView.setNotes(mFretTrack.fretEvents.get(mCurrentFretEvent).fretNotes,0);
        mTicksPerQtrNote = tpqn;
        mPlaying = false;
        mFretTrackView.invalidate();   // Force redraw
        mTempoText.setText(String.valueOf(tempo));
        playPauseButton.setVisibility(View.VISIBLE);
    }
}
