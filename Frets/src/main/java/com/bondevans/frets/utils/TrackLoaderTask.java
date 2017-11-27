package com.bondevans.frets.utils;

import android.os.AsyncTask;

import com.bondevans.frets.fretview.FretEvent;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;

import java.io.File;

/**
 * Async Wrapper for FileLoader
 */
public class TrackLoaderTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = TrackLoaderTask.class.getSimpleName();
    private final File file;
    private FileLoadedListener fileLoadedListener;
    private FretTrack mFretTrack;

    public TrackLoaderTask(File file) {
        Log.d(TAG, file.getAbsolutePath());
        this.file = file;
    }

    public interface FileLoadedListener {
        void OnFileLoaded(FretTrack fretTrack);

        void OnError(String msg);
    }

    public void setFileLoadedListener(FileLoadedListener fileLoadedListener) {
        this.fileLoadedListener = fileLoadedListener;
    }

    @Override
    protected String doInBackground(Void... params) {
        // Now load up the file contents
        try {
            mFretTrack = new FretTrack(FileLoader.loadFile(file));
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
        if (s.isEmpty()) {
            Log.d(TAG, "File loaded OK");
            fileLoadedListener.OnFileLoaded(mFretTrack);
        } else {
            fileLoadedListener.OnError(s);
        }
    }
}
