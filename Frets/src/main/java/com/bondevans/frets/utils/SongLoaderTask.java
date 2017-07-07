package com.bondevans.frets.utils;

import android.os.AsyncTask;

import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretviewer.TrackMerger;

import java.io.File;
import java.io.IOException;

/**
 * Async Wrapper for FileLoader
 */
public class SongLoaderTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = SongLoaderTask.class.getSimpleName();
    private final File songFile;
    private SongLoadedListener songLoadedListener;
    private FretSong mFretSong;

    public SongLoaderTask(File file) {
        this.songFile = file;
    }

    public interface SongLoadedListener {
        void OnFileLoaded(FretSong fretSong);
        void OnError(String msg);
    }

    public void setSongLoadedListener(SongLoadedListener songLoadedListener) {
        this.songLoadedListener = songLoadedListener;
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
            songLoadedListener.OnFileLoaded(mFretSong);
        } else {
            songLoadedListener.OnError(s);
        }
    }
}
