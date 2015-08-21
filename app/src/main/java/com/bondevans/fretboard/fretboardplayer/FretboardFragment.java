package com.bondevans.fretboard.fretboardplayer;

import android.app.Activity;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static android.net.Uri.*;


/**
 * A placeholder fragment containing a simple view.
 */
public class FretboardFragment extends Fragment {
    private static final String TAG = "FretboardFragment";
    FretboardView mFretboardView;

    public FretboardFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View myView = inflater.inflate(R.layout.fragment_main, container, false);
        mFretboardView = (FretboardView) myView.findViewById(R.id.fretboard);

        return myView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Uri uri = Uri.fromFile(new File("/sdcard/midi/test.mid"));
        loadFile("/sdcard/midi/test.mid", 1);
    }

    public void loadFile(String midiFile, int track){
        // convert MIDI file into list of fretboard events
        MidiFile mf = null;
        try {
            mf = new MidiFile(midiFile);
            String[] name = mf.getTrackNames();
            for(String x: name){
                Log.d(TAG,"Trackname: "+x);
            }
            List<FretEvent> evs = mf.loadFretEvents(1);
            Log.d(TAG, "Got "+evs.size()+" events");
        } catch (FretBoardException | IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }
}
