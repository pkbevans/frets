package com.bondevans.fretboard.fretviewer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.freteditor.FretEditActivity;
import com.bondevans.fretboard.fretview.FretSong;

import java.io.File;

public class FretViewActivity extends Activity {

    private static final String TAG = FretViewActivity.class.getSimpleName();
    public static final String INTENT_SONGCONTENTS = "adfgfdg";
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4522;
    private static final int REQUEST_EDIT_FRET = 678;
    private FretViewFragment fragment;

    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFileAccessPermission();
        setContentView(R.layout.activity_main);
        fragment = (FretViewFragment) getFragmentManager()
                .findFragmentById(R.id.fragment);

        if (savedInstanceState == null) {
            //  We should have the song contents in the intent
            Intent intent = getIntent();
            String songContents = intent.getStringExtra(INTENT_SONGCONTENTS);
            if (songContents == null) {
                Log.e(TAG, "Got File");
                fragment.setFretSong(new File(intent.getData().getPath()));
            } else {
                Log.d(TAG, "Got songcontents");
                fragment.setFretSong(new FretSong(songContents));
            }
        }
    }

    private void checkFileAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "checkFileAccessPermission 1");
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "checkFileAccessPermission 2");
                // Need to request permission from the user
                String[] perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(perms, REQUEST_CODE_READ_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fretview_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_edit) {
            showFretEdit();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showFretEdit() {
        Intent intent = new Intent(this, FretEditActivity.class);
        intent.putExtra(FretEditActivity.INTENT_FRETSONG, fragment.getFretSong().toString());
        // Add the file location into the intent, so that the editor can update the file
        Log.d(TAG, "setting data: " + getIntent().getDataString());
        intent.setData(getIntent().getData());

        try {
            startActivityForResult(intent, REQUEST_EDIT_FRET);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretEditActivity");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "HELLO on SaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "HELLO onActivityResult-activity request=[" + requestCode + "]result=[" + resultCode + "]");
        if (requestCode == REQUEST_EDIT_FRET && resultCode == FretEditActivity.RESULT_EDITED) {
            Log.d(TAG, "HELLO EDIT_FRET Finished");
            // Reload the fretTrack because it has been edited
            fragment.setFretSong(new File(getIntent().getData().getPath()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
