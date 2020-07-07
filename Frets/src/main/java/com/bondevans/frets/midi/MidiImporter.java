package com.bondevans.frets.midi;

import android.os.AsyncTask;
import android.util.Log;

import com.bondevans.frets.exception.EmptyTrackException;
import com.bondevans.frets.exception.FretboardException;
import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.fretview.FretNote;
import com.bondevans.frets.fretview.FretPosition;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.instruments.FretInstrument;
import com.bondevans.frets.utils.FileWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MidiImporter extends AsyncTask<Void, Integer, String> {
    private static final String TAG = MidiImporter.class.getSimpleName();
    private static final int DEFAULT_FRET_INSTRUMENT = 0;
    private final File mOutFile;
    private File mMidiFilePath;
    private MidiFile mMidiFile;
    private FileImportedListener fileImportedListener;

    public interface FileImportedListener {
        void OnImportedLoaded(File file);
        void OnError(String msg);
    }

    public void setFileImportedListener(FileImportedListener fileImportedListener) {
        this.fileImportedListener = fileImportedListener;
    }

    public MidiImporter(File inFilePath, File mOutFile) {
        this.mMidiFilePath = inFilePath;
        this.mOutFile = mOutFile;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "Loading header onPreExecute");
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d(TAG, "Loading header: " + mMidiFilePath);
        try {
            FretSong fretSong;
            List<MidiTrack> mTracks;
            mMidiFile = new MidiFile(mMidiFilePath);
            mTracks = mMidiFile.getTracks();

            fretSong = new FretSong(mMidiFile.getSongTitle(), "", mMidiFile.getTicksPerQtrNote(), mMidiFile.getBPM(), null);
            for (int i = 0; i < mTracks.size(); i++) {

                try {
                    fretSong.addTrack(loadTrack(mTracks.get(i).name, mTracks.get(i).index));
                } catch (EmptyTrackException e) {
                    Log.d(TAG, "Ignoring Empty track: " + mTracks.get(i).name);
                }
            }
            // Add an additional "click" track.  This is a track that simply has an dummy event on every beat
            // Get length in ticks of longest track
            int longest=0;
            for(FretTrack fretTrack: fretSong.getFretTracks()){
                longest=fretTrack.getTotalTicks()>longest?fretTrack.getTotalTicks():longest;
                Log.d(TAG, "HELLO Longest: "+longest);
            }
            Log.d(TAG, "HELLO1");
            FretTrack clickTrack = new FretTrack("Click Track", null,
                    0, DEFAULT_FRET_INSTRUMENT, false, longest);
            clickTrack.createClickTrack(longest,fretSong.getTpqn());
            fretSong.addTrack(clickTrack);
            Log.d(TAG, "HELLO2");
            // Now write out to new file in app cache directory
            FileWriter.writeFile(mOutFile, fretSong.toString());

        } catch (FretboardException e) {
            Log.d(TAG, e.getMessage());
            return e.getMessage();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            return e.getMessage();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String errorMessage) {
        if (errorMessage.isEmpty()) {
            fileImportedListener.OnImportedLoaded(mOutFile);
        } else {
            fileImportedListener.OnError(errorMessage);
        }
        Log.d(TAG, "onPostExecute");
    }

    // Load track
    private FretTrack loadTrack(String name, int track) throws IOException, EmptyTrackException {
        List<FretEvent> fretEvents;
        Log.d(TAG, "Loading track: " + name+ " (" + track+")");
        // See if we can work out what sort of instrument to assign to this track
        boolean isDrums = false;
        int midiSound = 0;
        int fretInstrument = FretInstrument.INTRUMENT_GUITAR;
        if(name.toLowerCase().matches(".*"+"drum"+".*")){
            isDrums=true;
        }else if(name.toLowerCase().matches(".*"+"bass"+".*")){
            midiSound=33;
            fretInstrument = FretInstrument.INTRUMENT_BASS;
        }else if(name.toLowerCase().matches(".*"+"guitar"+".*")){
            midiSound=29;
        }
        // convert MIDI file into list of fretboard events
        List<MidiNoteEvent> midiNoteEvents = new ArrayList<>();
        try {
            mMidiFile.loadNoteEvents(track, midiNoteEvents);
            Log.d(TAG, "Got " + midiNoteEvents.size() + " events");
        } catch (FretboardException e) {
            Log.e(TAG, "ERROR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        //  Loop through list of events.  For each set of events with the same time (i.e. chords)
        //  get the fret positions
        Log.d(TAG, "Loading Fret Events");
        fretEvents = new ArrayList<>();
        boolean first = true;
        FretPosition fp = new FretPosition(fretInstrument);
        List<FretNote> fretNotes = new ArrayList<>();
        int deltaTime = 0;
        int tempo = 0;
        int bend = 0;
        boolean hasNotes = false;
        int totalTicks=0;
        for (MidiNoteEvent ev : midiNoteEvents) {
            if (!first && ev.deltaTime > 0) {
                // If we get an event with a delay then get fret positions for the previous set,
                // reset count to zero and start building the next set.
//              Log.d(TAG, "Getting positions for [" + count + "] notes");
                fretNotes = fp.setDefaultFretPositions(fretNotes);
                totalTicks+=deltaTime;
                fretEvents.add(new FretEvent(deltaTime, fretNotes, tempo, bend, totalTicks));
                // save delay time for later
                deltaTime = ev.deltaTime;
                //reset the list of notes
                fretNotes = new ArrayList<>();
                tempo = 0;
                bend = 0;
            }
            else if(first){
                deltaTime = ev.deltaTime;
            }
//          Log.d(TAG, "Got event [" + ev.on + "][" + ev.name + "][" + ev.note + "]["+ev.instrument+"]");
            first = false;
            if (ev.type == MidiNoteEvent.TYPE_NOTE) {
                hasNotes = true;
                fretNotes.add(new FretNote(ev.note, ev.on));
            } else if (ev.type == MidiNoteEvent.TYPE_BEND) {
                bend = ev.bend;
            } else {
                tempo = ev.tempo;
            }
        }
        // Don't forget the last one - and dont add one if there weren't any events (first=true)
        if (!first) {
//          Log.d(TAG, "Getting positions for [" + count + "] notes (Last one)");
            fretEvents.add(new FretEvent(deltaTime, fp.setDefaultFretPositions(fretNotes), tempo, bend, totalTicks));
        }
        // If no FretEvents at all then we want to ignore this track (throw an exception)
        if (fretEvents.isEmpty() || !hasNotes) {
            throw new EmptyTrackException("Empty");
        }
        Log.d(TAG, "Got [" + fretEvents.size() + "] FretEvents");

        return new FretTrack(name, fretEvents, midiSound, fretInstrument, isDrums, totalTicks);
    }
}
