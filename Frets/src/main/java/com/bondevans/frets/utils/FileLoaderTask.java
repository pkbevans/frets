package com.bondevans.frets.utils;

import android.os.AsyncTask;

import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretviewer.TrackMerger;

import java.io.File;

/**
 * Async Wrapper for FileLoader
 */
public class FileLoaderTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = FileLoaderTask.class.getSimpleName();
    private final File file;
    private FileLoadedListener fileLoadedListener;
    private FretSong mFretSong;
    private boolean merge;

    public FileLoaderTask(File file, boolean merge) {
        this.file = file;
        this.merge = merge;
    }

    public interface FileLoadedListener {
        void OnFileLoaded(FretSong fretSong);

        void OnError(String msg);
    }

    public void setFileLoadedListener(FileLoadedListener fileLoadedListener) {
        this.fileLoadedListener = fileLoadedListener;
    }

    @Override
    protected String doInBackground(Void... params) {
        // Now load up the file contents
        try {
            mFretSong = new FretSong(FileLoader.loadFile(file));
            if(merge){
                doMerge();
            }
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
        if (s.isEmpty()) {
            Log.d(TAG, "File loaded OK");
            fileLoadedListener.OnFileLoaded(mFretSong);
        } else {
            fileLoadedListener.OnError(s);
        }
    }

    private void doMerge() {
        // Start off with the solo track
        mFretSong.getTrack(mFretSong.getSoloTrack()).dump("BEFORE");
        TrackMerger trackMerger = new TrackMerger(mFretSong.getTrack(mFretSong.getSoloTrack()).fretEvents, mFretSong.getSoloTrack());
        // then merge in all the other tracks
        int track=0;
        while(track<mFretSong.tracks()){
            if(track!=mFretSong.getSoloTrack()) {//dont want the solo track twice
                mFretSong.getTrack(track).dump("MERGING IN track:"+track);
                trackMerger.mergeTrack(mFretSong.getTrack(track).fretEvents, track);
                mFretSong.getTrack(mFretSong.getSoloTrack()).dump("AFTER merging track:"+track);
            }
            ++track;
        }
        mFretSong.getTrack(mFretSong.getSoloTrack()).dump("END");
    }
}
