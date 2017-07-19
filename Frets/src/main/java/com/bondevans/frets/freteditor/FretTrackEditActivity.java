package com.bondevans.frets.freteditor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.fretview.FretTrack;
import com.bondevans.frets.utils.FileWriterTask;
import com.bondevans.frets.utils.Log;
import com.bondevans.frets.utils.TrackLoaderTask;

import java.io.File;

public class FretTrackEditActivity extends AppCompatActivity {
    private static final String TAG = FretTrackEditActivity.class.getSimpleName();
    public static final int RESULT_EDITED = 1;
    public static final int RESULT_NOT_EDITED = 0;
    private static final String SAVE_FILE = "sdkfjhi";
    private ProgressBar progressBar;
    private FretTrackEditFragment fretTrackEditFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate");
        setContentView(R.layout.frettrackedit_activity);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        fretTrackEditFragment = (FretTrackEditFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        if (savedInstanceState == null) {
            //  We should have the track file in the intent
            Intent intent = getIntent();
            Log.d(TAG, "Got File");
            loadFretTrack(new File(intent.getData().getPath()),intent.getIntExtra(FretSongEditActivity.KEY_EDITED_TRACK,0));
        }
        else{
            Log.d(TAG, "CONFIG CHANGE");
            // TODO Something probably
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
            case R.id.action_save_song:
                saveTrack(false);
                break;
            case R.id.action_settings:
                // TODO Either allow settings to be accessed from here or remove this option
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        // Save if necessary
        saveTrack(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "HELLO on SaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    private void saveTrack(final boolean finish) {
        Log.d(TAG, "saveTrack");

        if (fretTrackEditFragment.isEdited()) {
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "saving Track");
            final File file = new File(getIntent().getData().getPath());
            Log.d(TAG, "HELLO Writing back to file[" + file.toString() + "]");
            FileWriterTask fileWriterTask = new FileWriterTask(file, fretTrackEditFragment.mFretTrack.toString());
            fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
                @Override
                public void OnFileWritten() {
                    progressBar.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "Update written to file: " + file.getName());
                    FretTrackEditActivity.this.setResult(RESULT_EDITED);
                    if (finish) {
                        FretTrackEditActivity.this.finish();
                    }
                }

                @Override
                public void OnError(String msg) {
                    Log.d(TAG, "File write failed: " + file.getName() + " " + msg);
                    Toast.makeText(FretTrackEditActivity.this, R.string.save_failed, Toast.LENGTH_LONG).show();
                    FretTrackEditActivity.this.setResult(RESULT_NOT_EDITED);
                    if (finish) {
                        FretTrackEditActivity.this.finish();
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
                // Save track and close activity
                saveTrack(true);
            }

            @Override
            public void onCancel() {
                FretTrackEditActivity.this.setResult(RESULT_NOT_EDITED);
                FretTrackEditActivity.this.finish();
            }
        });
        saveFileDialog.show(getSupportFragmentManager(), SAVE_FILE);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Set the <code>FretTrack</code>
     *
     * @param file temporary Track file
     * @param track Track number (not curently used)
     */
    public void loadFretTrack(File file, final int track) {
        Log.d(TAG, "setFretTrack");
        progressBar.setVisibility(View.VISIBLE);
        TrackLoaderTask trackLoaderTask = new TrackLoaderTask(file);
        trackLoaderTask.setFileLoadedListener(new TrackLoaderTask.FileLoadedListener() {
            @Override
            public void OnFileLoaded(FretTrack fretTrack) {
                progressBar.setVisibility(View.INVISIBLE);
                Log.d(TAG, "setFretTrack file loaded");
                fretTrackEditFragment.setFretTrack(fretTrack);
            }
            @Override
            public void OnError(String msg) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(FretTrackEditActivity.this, R.string.load_failed+msg, Toast.LENGTH_LONG).show();
                Log.d(TAG, "setFretTrack file load ERROR: "+msg);
            }
        });
        trackLoaderTask.execute();
    }
 }