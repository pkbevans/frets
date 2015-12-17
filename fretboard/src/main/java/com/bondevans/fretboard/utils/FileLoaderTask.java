package com.bondevans.fretboard.utils;

import android.os.AsyncTask;

import java.io.File;

/**
 * Async Wrapper for FileWriter
 */
public class FileLoaderTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = FileLoaderTask.class.getSimpleName();
    private final File file;
    private FileLoadedListener fileLoadedListener;
    private String mContents;

    public FileLoaderTask(File file) {
        this.file = file;
    }

    public interface FileLoadedListener {
        void OnFileLoaded(String contents);

        void OnError(String msg);
    }

    public void setFileLoadedListener(FileLoadedListener fileLoadedListener) {
        this.fileLoadedListener = fileLoadedListener;
    }

    @Override
    protected String doInBackground(Void... params) {
        // Now load up the file contents
        try {
            mContents = FileLoader.loadFile(file);
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
        if (s.isEmpty()) {
            Log.d(TAG, "File loaded OK");
            fileLoadedListener.OnFileLoaded(mContents);
        } else {
            fileLoadedListener.OnError(s);
        }
    }
}
