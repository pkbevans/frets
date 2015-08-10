package com.bondevans.fretboard.fretboardplayer;

import android.app.Activity;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

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
        loadFile(uri,0);
    }

    public void loadFile(Uri midiFile, int track){
        // convert MIDI file into list of fretboard events
        MidiFile mf = new MidiFile(midiFile);
        mf.loadFretEvents(0);
    }
}
