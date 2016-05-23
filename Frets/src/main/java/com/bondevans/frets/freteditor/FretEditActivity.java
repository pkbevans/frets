package com.bondevans.frets.freteditor;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.utils.FileWriterTask;
import com.bondevans.frets.utils.Log;
import com.firebase.client.Firebase;

import java.io.File;

public class FretEditActivity extends AppCompatActivity {
    private static final String TAG = FretEditActivity.class.getSimpleName();
    private static final String TAG_SONGDETAILS = "SongDets";
    private static final String SAVE_FILE = "sdkfjhi";
    public static final int RESULT_EDITED = 1;
    public static final int RESULT_NOT_EDITED = 0;
    private FretEditFragment mFragment;
    private Firebase mFirebaseRef;

    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fretedit_activity);
        mFirebaseRef = new Firebase(getString(R.string.firebase_url));
        mFragment = (FretEditFragment) getFragmentManager()
                .findFragmentById(R.id.fragment);

        if (savedInstanceState == null) {
            //  We should have the song file in the intent
            Intent intent = getIntent();
            Log.d(TAG, "Got File");
            mFragment.setFretSong(new File(intent.getData().getPath()));
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fretedit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
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

    private void publishSong() {
        // Get Song Details
        updateSongDetails();
    }

    public void updateSongDetails() {
        // Get the Song name and description
        SongDetailsDialog songDetailsDialog = SongDetailsDialog.newInstance(mFragment.mFretSong.getName());
        songDetailsDialog.setSongDetailsListener(new SongDetailsDialog.SongDetailsListener() {
            @Override
            public void OnSongDetailsEntered(String name, String description) {
                mFragment.mFretSong.setName(name);// TODO Dont need to allow edit of song name again in this dialog
                // Write to server
                writeSongToServer(mFragment.getFretSong());
            }

            @Override
            public void OnCancel() {
            }
        });
        songDetailsDialog.show(getFragmentManager(), TAG_SONGDETAILS);
    }

    private void writeSongToServer(FretSong fretSong) {
        FBWrite.addSong(mFirebaseRef, fretSong);
        Toast.makeText(FretEditActivity.this, fretSong.getName() + getString(R.string.published), Toast.LENGTH_SHORT).show();
        finish();   //Lets get outta here
    }

    private void saveSong(final boolean finish) {
        Log.d(TAG, "saveSong");

        if (mFragment != null && mFragment.isEdited()) {
            Log.d(TAG, "saving Song");
            final File file = new File(getIntent().getData().getPath());
            Log.d(TAG, "HELLO Writing to file[" + file.toString() + "]");
            FileWriterTask fileWriterTask = new FileWriterTask(file, mFragment.getFretSong().toString());
            fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
                @Override
                public void OnFileWritten() {
                    Log.d(TAG, "Update written to file: " + file.getName());
                    Toast.makeText(FretEditActivity.this, getString(R.string.fret_saved) + file.getPath(), Toast.LENGTH_SHORT).show();
                    FretEditActivity.this.setResult(RESULT_EDITED);
                    if (finish) {
                        FretEditActivity.this.finish();
                    } else {
                        mFragment.setEdited(false);
                    }
                }

                @Override
                public void OnError(String msg) {
                    Log.d(TAG, "File write failed: " + file.getName() + " " + msg);
                    Toast.makeText(FretEditActivity.this, R.string.save_failed, Toast.LENGTH_LONG).show();
                    FretEditActivity.this.setResult(RESULT_NOT_EDITED);
                    if (finish) {
                        FretEditActivity.this.finish();
                    }
                }
            });
            fileWriterTask.execute();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mFragment.isEdited()) {
            Log.d(TAG, "HELLO - BACK PRESSED and SONG CHANGED");
            showSaveFileDialog();
            return true;
        }
        Log.d(TAG, "HELLO - BACK PRESSED2");
        return super.onKeyDown(keyCode, event);
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
                FretEditActivity.this.setResult(RESULT_NOT_EDITED);
                FretEditActivity.this.finish();
            }
        });
        saveFileDialog.show(getSupportFragmentManager(), SAVE_FILE);
    }
}
