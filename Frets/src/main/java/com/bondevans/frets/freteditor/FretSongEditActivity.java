package com.bondevans.frets.freteditor;

import android.content.Intent;
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
import com.bondevans.frets.utils.FileLoaderTask;
import com.bondevans.frets.utils.FileWriterTask;
import com.bondevans.frets.utils.Log;
import com.firebase.client.Firebase;

import java.io.File;

public class FretSongEditActivity extends AppCompatActivity implements
        FretSongEditFragment.OnTrackSelectedListener {
    private static final String TAG = FretSongEditActivity.class.getSimpleName();
    public static final int RESULT_EDITED = 1;
    public static final int RESULT_NOT_EDITED = 0;
    private static final String SAVE_FILE = "sdkfjhi";
    private static final String TAG_TRACKLIST = "tracList";
    private static final String TAG_TRACKEDIT = "trackedit";
    private static final String KEY_EDITED_TRACK = "et";
    private FretSongEditFragment fretSongEditFragment = null;
    private Firebase mFirebaseRef;
    private ProgressBar progressBar;
    private FretTrackEditFragment fretTrackEditFragment;
    private FretSong mFretSong;
    private int mTrackBeingEdited;

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
            // See if we've got a FretTrackEditFragment
            if(null==(fretTrackEditFragment = (FretTrackEditFragment) fm.findFragmentByTag(TAG_TRACKEDIT))) {
                Log.d(TAG, "No FretTrackEditFragment");
            }
            else{
                Log.d(TAG, "Found FretTrackEditFragment track="+mTrackBeingEdited);
                // Need to reload the track into the editor
                fretTrackEditFragment.setFretTrack(
                        fretSongEditFragment.getFretSong().getTrack(mTrackBeingEdited),
                        mTrackBeingEdited );
            }
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
            case R.id.action_save_song:
                saveSong(false);
                break;
            case R.id.action_settings:
                // TODO Either allow settings to be accessed from here or remove this option
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // FretTrackEdit fragment is active
            Log.d(TAG, "onBackPressed and BACKSTACK count > 0");
            if(fretTrackEditFragment.isEdited()) {
                fretSongEditFragment.setEdited(true);
            }
            getSupportFragmentManager().popBackStack();
            getSupportActionBar().setTitle(getString(R.string.edit_song));
        }
        else if(fretSongEditFragment.isEdited()) {
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
        writeSongToServer(fretSongEditFragment.getFretSong());
    }

    private void writeSongToServer(FretSong fretSong) {
        FBWrite.addSong(mFirebaseRef, fretSong);
        Toast.makeText(FretSongEditActivity.this, fretSong.getName() + getString(R.string.published), Toast.LENGTH_SHORT).show();
        finish();   //Lets get outta here
    }

    private void saveSong(final boolean finish) {
        Log.d(TAG, "saveSong");

        if (fretSongEditFragment != null && fretSongEditFragment.isEdited()) {
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
        fretTrackEditFragment = new FretTrackEditFragment();
        fretTrackEditFragment.setFretTrack(fretSongEditFragment.getFretSong().getTrack(track), track);
        // Add the fragment to the activity, pushing this transaction
        // on to the back stack.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.edit_frag, fretTrackEditFragment, TAG_TRACKEDIT);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();
        getSupportActionBar().setTitle(getString(R.string.edit_track));
        fretSongEditFragment.setEdited(true);
    }

    /**
     * Load up <code>FretSong</code> from give file.
     * @param file Song file from cache
     */
    private void setFretSong(File file) {
        Log.d(TAG, "setFretSong file");
        progressBar.setVisibility(View.VISIBLE);
        FileLoaderTask fileLoaderTask = new FileLoaderTask(file, false);
        fileLoaderTask.setFileLoadedListener(new FileLoaderTask.FileLoadedListener() {
            @Override
            public void OnFileLoaded(FretSong fretSong) {
                Log.d(TAG, "setFretSong file loaded");
                mFretSong = fretSong;
                fretSongEditFragment.setFretSong(mFretSong);
                getSupportActionBar().setTitle(getString(R.string.edit_song));
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void OnError(String msg) {
                Toast.makeText(FretSongEditActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        fileLoaderTask.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}