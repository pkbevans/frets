package com.bondevans.fretboard.utils;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

/**
 * Async Wrapper for FileWriter
 */
public class FileWriterTask extends AsyncTask<Void, Void,String> {
    private static final String TAG = FileWriterTask.class.getSimpleName();
    private final File file;
    private final String contents;
    private FileWrittenListener fileWrittenListener;

    public FileWriterTask(File file, String contents){
        this.file = file;
        this.contents = contents;
    }
    public interface FileWrittenListener {
        void OnFileWritten();
        void OnError(String msg);
    }

    public void setFileWrittenListener(FileWrittenListener fileWrittenListener){
        this.fileWrittenListener = fileWrittenListener;
    }

    @Override
    protected String doInBackground(Void... params) {
        // Now write out to new file in app cache directory
        try {
            FileWriter.writeFile(file, contents);
        } catch (IOException e) {
            return e.getMessage();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
        if(s.isEmpty()){
            Log.d(TAG, "File written OK");
            fileWrittenListener.OnFileWritten();
        }
        else{
            fileWrittenListener.OnError(s);
        }
    }
}
