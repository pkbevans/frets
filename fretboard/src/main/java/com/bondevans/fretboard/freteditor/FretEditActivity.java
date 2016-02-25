package com.bondevans.fretboard.freteditor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.fretview.FretSong;
import com.bondevans.fretboard.utils.FileWriterTask;
import com.bondevans.fretboard.utils.Log;

import java.io.File;

public class FretEditActivity extends Activity {
    private static final String TAG = FretEditActivity.class.getSimpleName();
    public static final String INTENT_FRETSONG = "fretsong";
    private static final String SAVE_FILE = "sdkfjhi";
    public static final int RESULT_EDITED = 1;
    public static final int RESULT_NOT_EDITED = 0;
    private FretEditFragment mFragment;

    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fretedit_activity);
        mFragment = (FretEditFragment) getFragmentManager()
                .findFragmentById(R.id.fragment);

        if (savedInstanceState == null) {
            //  We should have the song contents in the intent
            Intent intent = getIntent();
            Log.d(TAG, "HELLO track IN[" + intent.getStringExtra(INTENT_FRETSONG) + "]");
            mFragment.setFretSong(new FretSong(intent.getStringExtra(INTENT_FRETSONG))); // Assumes only 1 track (or the one we want is first one)
        }
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

        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    private void saveSong() {
        Log.d(TAG, "saveSong");

        if (mFragment != null && mFragment.isEdited()) {
            Log.d(TAG, "saving Song");
            final File file = new File(getIntent().getData().getPath());
            Log.d(TAG, "HELLO track OUT[" + mFragment.getFretSong().toString() + "]");
            FileWriterTask fileWriterTask = new FileWriterTask(file, mFragment.getFretSong().toString());
            fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
                @Override
                public void OnFileWritten() {
                    Log.d(TAG, "Update written to file: " + file.getName());
                    Toast.makeText(FretEditActivity.this, R.string.fret_saved, Toast.LENGTH_SHORT).show();
                    FretEditActivity.this.setResult(RESULT_EDITED);
                    FretEditActivity.this.finish();
                }

                @Override
                public void OnError(String msg) {
                    Log.d(TAG, "File write failed: " + file.getName() + " " + msg);
                    Toast.makeText(FretEditActivity.this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                    FretEditActivity.this.setResult(RESULT_NOT_EDITED);
                    FretEditActivity.this.finish();
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
                saveSong();
            }

            @Override
            public void onCancel() {
                FretEditActivity.this.setResult(RESULT_NOT_EDITED);
                FretEditActivity.this.finish();
            }
        });
        saveFileDialog.show(getFragmentManager(), SAVE_FILE);
    }
}
