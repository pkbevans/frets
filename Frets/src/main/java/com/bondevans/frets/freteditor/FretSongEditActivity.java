package com.bondevans.frets.freteditor;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.fretviewer.FretViewActivity;
import com.bondevans.frets.fretviewer.TrackMerger;
import com.bondevans.frets.utils.FileWriter;
import com.bondevans.frets.utils.FileWriterTask;
import com.bondevans.frets.utils.Log;
import com.bondevans.frets.utils.SongLoaderTask;
import com.bondevans.frets.utils.TrackLoaderTask;
import com.firebase.client.Firebase;

import java.io.File;
import java.io.IOException;

public class FretSongEditActivity extends AppCompatActivity implements
        FretSongEditFragment.OnTrackSelectedListener {
    private static final String TAG = FretSongEditActivity.class.getSimpleName();
    public static final int RESULT_EDITED = 1;
    public static final int RESULT_NOT_EDITED = 0;
    private static final String SAVE_FILE = "sdkfjhi";
    private static final String TAG_TRACKLIST = "tracList";
    public static final String  KEY_EDITED_TRACK = "et";
    private static final int REQUEST_EDIT_TRACK = 8768;
    private static final int MAX_TRACKS = 2;
    private static final String TOOMANYTRACKS = "7yytyi8";
    private FretSongEditFragment fretSongEditFragment = null;
    private Firebase mFirebaseRef;
    private ProgressBar progressBar;
    private int mTrackBeingEdited;
    private File mTrackTmpFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate");
        setContentView(R.layout.fretsongedit_activity);
        // Add the song edit fragment first, which will then get overwritten with the track edit fragment
        // when a track is selected.
        // Check whether we already have a frag (i.e.) on Configuration change
        FragmentManager fm = getSupportFragmentManager();
        if(null==(fretSongEditFragment = (FretSongEditFragment) fm.findFragmentByTag(TAG_TRACKLIST))) {
            fretSongEditFragment = new FretSongEditFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.edit_frag, fretSongEditFragment, TAG_TRACKLIST); // f1_container is your FrameLayout container
            ft.commit();
        }

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mFirebaseRef = new Firebase(getString(R.string.firebase_url));

        if (savedInstanceState == null) {
            //  We should have the song file in the intent
            Intent intent = getIntent();
            Log.d(TAG, "Got File");
            setFretSong(new File(intent.getData().getPath()));
        }
        else{
            Log.d(TAG, "CONFIG CHANGE");
            mTrackBeingEdited=savedInstanceState.getInt(KEY_EDITED_TRACK);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onCreateOptionsMenu(com.actionbarsherlock.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fretedit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
                // Back to Songs
                onBackPressed();
                break;
            case R.id.action_publish:
                publishSong();
                break;
            case R.id.action_preview_song:
                previewSong();
                break;
            case R.id.action_save_song:
                saveSong(false);
                break;
            case R.id.action_settings:
                // Either allow settings to be accessed from here or remove this option
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void previewSong() {
        Intent intent = new Intent(this, FretViewActivity.class);
        intent.setData(Uri.fromFile(new File(getIntent().getData().getPath())));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretViewActivity");
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if(fretSongEditFragment.isEdited()) {
            Log.d(TAG, "onBackPressed and SONG CHANGED");
            showSaveFileDialog();
        }
        else {
            Log.d(TAG, "HELLO - BACK PRESSED and calling super");
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "HELLO on SaveInstanceState");
        outState.putInt(KEY_EDITED_TRACK, mTrackBeingEdited);
        super.onSaveInstanceState(outState);
    }

    private void publishSong() {
        // Merge tracks into solo track and remove FretEvents from other tracks
        mergeTracks();
        // Write song to FireBase
        FBWrite.addSong(mFirebaseRef, fretSongEditFragment.getFretSong());
        Toast.makeText(FretSongEditActivity.this, fretSongEditFragment.getFretSong().getName() + getString(R.string.published), Toast.LENGTH_SHORT).show();
        finish();   //Lets get outta here
    }

    private void saveSong(final boolean finish) {
        Log.d(TAG, "saveSong");

        // Only allow two tracks
        if(getSong().tracks()>MAX_TRACKS){
            TooManyTracksDialog tooManyTracksDialog = new TooManyTracksDialog();
            tooManyTracksDialog.show(getSupportFragmentManager(), TOOMANYTRACKS);
        }else if (fretSongEditFragment != null && fretSongEditFragment.isEdited()) {
            Log.d(TAG, "saving Song");
            final File file = new File(getIntent().getData().getPath());
            Log.d(TAG, "HELLO Writing to file[" + file.toString() + "]");
            FileWriterTask fileWriterTask = new FileWriterTask(file, fretSongEditFragment.getFretSong().toString());
            fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
                @Override
                public void OnFileWritten() {
                    Log.d(TAG, "Update written to file: " + file.getName());
                    Toast.makeText(FretSongEditActivity.this, getString(R.string.fret_saved) + file.getPath(), Toast.LENGTH_SHORT).show();
                    FretSongEditActivity.this.setResult(RESULT_EDITED);
                    if (finish) {
                        FretSongEditActivity.this.finish();
                    } else {
                        fretSongEditFragment.setEdited(false);
                    }
                }

                @Override
                public void OnError(String msg) {
                    Log.d(TAG, "File write failed: " + file.getName() + " " + msg);
                    Toast.makeText(FretSongEditActivity.this, R.string.save_failed, Toast.LENGTH_LONG).show();
                    FretSongEditActivity.this.setResult(RESULT_NOT_EDITED);
                    if (finish) {
                        FretSongEditActivity.this.finish();
                    }
                }
            });
            fileWriterTask.execute();
        }
    }

    private void saveTrackToTempFile(final File file, final int track) {
        Log.d(TAG, "saveTrack");

            Log.d(TAG, "HELLO Writing to file[" + file.toString() + "]");
            FileWriterTask fileWriterTask = new FileWriterTask(file, fretSongEditFragment.getFretSong().getTrack(track).toString());
            fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
                @Override
                public void OnFileWritten() {
                    Log.d(TAG, "Track written to file: " + file.getName());
                    // Launch FretTrackEditActivity for return and wait for the return
                    showTrackEdit(file, track);
                }

                @Override
                public void OnError(String msg) {
                    Log.d(TAG, "File write failed: " + file.getName() + " " + msg);
                    Toast.makeText(FretSongEditActivity.this, R.string.save_failed, Toast.LENGTH_LONG).show();
                }
            });
            fileWriterTask.execute();
    }

    private void showSaveFileDialog() {

        SaveFileDialog saveFileDialog = new SaveFileDialog();
        saveFileDialog.SetSaveFileListener(new SaveFileDialog.SaveFileListener() {
            @Override
            public void onSave() {
                saveSong(true);
            }

            @Override
            public void onCancel() {
                FretSongEditActivity.this.setResult(RESULT_NOT_EDITED);
                FretSongEditActivity.this.finish();
            }
        });
        saveFileDialog.show(getSupportFragmentManager(), SAVE_FILE);
    }

    @Override
    public void onTrackSelected(int track) {
        // Instantiate a new fragment.
        mTrackBeingEdited = track;
        // Write out the track to a temporary file
        mTrackTmpFile = new File(this.getExternalFilesDir(null), fretSongEditFragment.getFretSong().getName()+"-Track-"+track+ "tmp.xml");
        saveTrackToTempFile(mTrackTmpFile, track);
        fretSongEditFragment.setEdited(true);
    }

    /**
     * Load up <code>FretSong</code> from give file.
     * @param file Song file from cache
     */
    private void setFretSong(File file) {
        Log.d(TAG, "setFretSong file");
        progressBar.setVisibility(View.VISIBLE);
        SongLoaderTask songLoaderTask = new SongLoaderTask(file);
        songLoaderTask.setSongLoadedListener(new SongLoaderTask.SongLoadedListener() {
            @Override
            public void OnFileLoaded(FretSong fretSong) {
                Log.d(TAG, "setFretSong file loaded");
                fretSongEditFragment.setFretSong(fretSong);
                getSupportActionBar().setTitle(getString(R.string.edit_song));
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void OnError(String msg) {
                Toast.makeText(FretSongEditActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        songLoaderTask.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void showTrackEdit(File file, int track) {
        Intent intent = new Intent(this, FretTrackEditActivity.class);
        // Add the file location into the intent, so that the editor can update the file
        Log.d(TAG, "setting data: " + Uri.fromFile(file).toString());
        intent.setData(Uri.fromFile(file));
        intent.putExtra(KEY_EDITED_TRACK, track);
        try {
            startActivityForResult(intent, REQUEST_EDIT_TRACK);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretTrackEditActivity");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "HELLO onActivityResult-activity request=[" + requestCode + "]result=[" + resultCode + "]");
        if (requestCode == REQUEST_EDIT_TRACK && resultCode == FretSongEditActivity.RESULT_EDITED) {
            Log.d(TAG, "HELLO EDIT_FRET Finished");
            // Reload the fretTrack because it has been edited
            TrackLoaderTask trackLoaderTask = new TrackLoaderTask(mTrackTmpFile);
            trackLoaderTask.setFileLoadedListener(new TrackLoaderTask.FileLoadedListener() {
                @Override
                public void OnFileLoaded(FretTrack fretTrack) {
                    Log.d(TAG, "setFretTrack file loaded");
                    fretSongEditFragment.getFretSong().updateTrack(mTrackBeingEdited, fretTrack);
                }
                @Override
                public void OnError(String msg) {
                    Log.d(TAG, "FretTrack load error: "+msg);
                    Toast.makeText(FretSongEditActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
            trackLoaderTask.execute();

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void mergeTracks() {
        // Start off with the solo track
        getSong().getTrack(getSong().getSoloTrack()).dump("BEFORE");
        TrackMerger trackMerger = new TrackMerger(getSong().getTrack(getSong().getSoloTrack()).fretEvents, getSong().getSoloTrack());
        // then merge in all the other tracks
        int track=0;
        while(track<getSong().tracks()){
            if(track!=getSong().getSoloTrack()) {//dont want the solo track twice
                getSong().getTrack(track).dump("MERGING IN track:"+track);
                trackMerger.mergeTrack(getSong().getTrack(track).fretEvents, track);
                getSong().getTrack(getSong().getSoloTrack()).dump("AFTER merging track:"+track);
                // Remove the events from the merged-in tracks.  No longer required.
                getSong().getTrack(track).removeEvents();
            }
            ++track;
        }
        getSong().getTrack(getSong().getSoloTrack()).dump("END");
        // TODO  (REMOVE) Write out the merged track - for debugging purposes only
        try {
            File tmpFile = new File(new File(getIntent().getData().getPath())+".merged.xml");
            FileWriter.writeFile(tmpFile, getSong().getTrack(getSong().getSoloTrack()).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private FretSong getSong(){
        return fretSongEditFragment.getFretSong();
    }
}