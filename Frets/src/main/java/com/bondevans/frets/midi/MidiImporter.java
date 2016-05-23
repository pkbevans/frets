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
import com.bondevans.frets.instruments.FretGuitarStandard;
import com.bondevans.frets.utils.FileWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MidiImporter extends AsyncTask<Void, Integer, String> {
    private static final String TAG = MidiImporter.class.getSimpleName();
    private static final String NO_ID_YET = "";
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

            fretSong = new FretSong(mMidiFile.getSongTitle(), mMidiFile.getTicksPerQtrNote(), mMidiFile.getBPM(), null);
            for (int i = 0; i < mTracks.size(); i++) {

                try {
                    fretSong.addTrack(loadTrack(mTracks.get(i).name, mTracks.get(i).index));
                } catch (EmptyTrackException e) {
                    Log.d(TAG, "Ignoring Empty track: " + mTracks.get(i).name);
                }
            }
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
        Log.d(TAG, "Loading track: " + track);
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
        FretPosition fp = new FretPosition(new FretGuitarStandard());
        List<FretNote> fretNotes = new ArrayList<>();
        int deltaTime = 0;
        int tempo = 0;
        int bend = 0;
        boolean hasNotes = false;
        for (MidiNoteEvent ev : midiNoteEvents) {
            if (!first && ev.deltaTime > 0) {
                // If we get an event with a delay then get fret positions for the previous set,
                // reset count to zero and start building the next set.
//              Log.d(TAG, "Getting positions for [" + count + "] notes");
                fretNotes = fp.getFretPositions(fretNotes);
                fretEvents.add(new FretEvent(deltaTime, fretNotes, tempo, bend));
                // calculate delay time and save for later
                deltaTime = ev.deltaTime;
                //reset the list of notes
                fretNotes = new ArrayList<>();
                tempo = 0;
                bend = 0;
            }
//          Log.d(TAG, "Got event [" + ev.on + "][" + ev.name + "][" + ev.note + "]["+ev.instrument+"]");
            first = false;
            if (ev.type == MidiNoteEvent.TYPE_NOTE) {
                hasNotes = true;
                fretNotes.add(new FretNote(ev.note, ev.on));
            } else if (ev.type == MidiNoteEvent.TYPE_BEND) {
                // If we get a pitch bend event then it applies to the previous (i.e. current) FretNote list
                // Get previous event
                bend = ev.bend;
            } else {
                tempo = ev.tempo;
            }
        }
        // Don't forget the last one - and dont add one if there weren't any events (first=true)
        if (!first) {
//          Log.d(TAG, "Getting positions for [" + count + "] notes (Last one)");
            fretEvents.add(new FretEvent(deltaTime, fp.getFretPositions(fretNotes), 0, bend));
        }
        // If no FretEvents at all then we want to ignore this track (throw an exception)
        if (fretEvents.isEmpty() || !hasNotes) {
            throw new EmptyTrackException("Empty");
        }
        Log.d(TAG, "Got [" + fretEvents.size() + "] FretEvents");
        return new FretTrack(name, fretEvents);
    }
}
