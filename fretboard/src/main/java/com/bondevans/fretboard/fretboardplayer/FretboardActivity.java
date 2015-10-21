package com.bondevans.fretboard.fretboardplayer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class FretboardActivity extends Activity {

    private static final String TAG = "FretboardActivity";
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4522;
    private static final String KEY_FILENAME = "MidiFileName";
    private String mFileName;
    private FretboardFragment fragment;

    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFileAccessPermission();
        //  See if we got a file name int he intent
        setContentView(R.layout.activity_main);
        fragment = (FretboardFragment) getFragmentManager()
                .findFragmentById(R.id.fragment);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent != null) {
                Uri x = intent.getData();
                if (x != null) {
                    mFileName = x.getPath();
                    fragment.setFileName(mFileName);
                }
            }
        }
        else {
            mFileName = savedInstanceState.getString(KEY_FILENAME);
            Log.d(TAG, "Got savedInstanceState: "+mFileName);
        }
    }

    private void checkFileAccessPermission() {
        Log.d(TAG, "checkFileAccessPermission 1");
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkFileAccessPermission 2");
            // Need to request permission from the user
            String[] perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(perms, REQUEST_CODE_READ_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // TODO need to handle user not allowing access.
        Log.d(TAG, "onRequestPermissionsResult");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "HELLO on SaveInstanceState");
//        outState.putString(KEY_FILENAME, mFileName);
        super.onSaveInstanceState(outState);
    }

}
