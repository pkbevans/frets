package com.bondevans.fretboard.fretview;

import android.os.AsyncTask;

import com.bondevans.fretboard.utils.FileWriter;
import com.bondevans.fretboard.utils.Log;

import java.io.File;

/**
 * Loads a FretSong from xml file on disk into a FretSong.
 */
public class FretSongLoader extends AsyncTask<Void, Integer, String> {
    private static final String TAG = FretSongLoader.class.getSimpleName();
    private final File file;
    private SongLoadedListener songLoadedListener;
    private FretSong mFretSong;

    public interface SongLoadedListener {
        void OnSongLoaded(FretSong song);
        void OnError(String msg);
    }

    public void setSongLoadedListener(SongLoadedListener songLoadedListener){
        this.songLoadedListener = songLoadedListener;
    }

    /**
     * Creates a new FretSongLoader with a given .xml file
     * @param file Song file to load
     */
    public FretSongLoader(File file) {
        this.file = file;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d(TAG, "Loading file: "+file.getName());
        // Load up FretSong class from file
        try {
            mFretSong = new FretSong(FileWriter.loadFile(file));
            return "";    // All OK
        } catch (Exception e) {
            return e.getMessage();  // Oops
        }
    }

    @Override
    protected void onPostExecute(String errorMessage) {
        if(errorMessage.isEmpty()) {
            songLoadedListener.OnSongLoaded(mFretSong);
        }
        else {
            songLoadedListener.OnError(errorMessage);
        }
    }
}
