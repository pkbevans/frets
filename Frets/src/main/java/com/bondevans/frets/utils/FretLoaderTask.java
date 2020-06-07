package com.bondevans.frets.utils;

import android.os.AsyncTask;

import com.bondevans.frets.fretview.FretSong;

import java.io.File;

/**
 * Async Wrapper for FileLoader
 */
public class FretLoaderTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = FretLoaderTask.class.getSimpleName();
    private final File songFile;
    private FretLoadedListener fretLoadedListener;
    private FretSong mFretSong;

    public FretLoaderTask(File file) {
        this.songFile = file;
    }

    public interface FretLoadedListener {
        void OnFileLoaded(FretSong fretSong);
        void OnError(String msg);
    }

    public void setFretLoadedListener(FretLoadedListener fretLoadedListener) {
        this.fretLoadedListener = fretLoadedListener;
    }

    @Override
    protected String doInBackground(Void... params) {
        // Now load up the file contents
        try {
            mFretSong = new FretSong(FileLoader.loadFile(songFile));
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
        if (s.isEmpty()) {
            Log.d(TAG, "File loaded OK");
            fretLoadedListener.OnFileLoaded(mFretSong);
        } else {
            fretLoadedListener.OnError(s);
        }
    }
}
