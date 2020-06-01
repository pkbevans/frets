package com.bondevans.frets.fretviewer;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.freteditor.FretSongEditActivity;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.utils.SongLoaderTask;

import java.io.File;

public class FretViewActivity extends AppCompatActivity {

    private static final String TAG = FretViewActivity.class.getSimpleName();
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4522;
    private static final int REQUEST_EDIT_FRET = 678;
    private FretViewFragment fragment;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFileAccessPermission();
        setContentView(R.layout.fretview_activity);
        fragment = (FretViewFragment) getFragmentManager().findFragmentById(R.id.fragment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        progressBar = findViewById(R.id.progress_bar); // Attaching the layout to the toolbar object

        if (savedInstanceState == null) {
            //  We should have the song contents in the intent
            Intent intent = getIntent();
            Log.d(TAG, "Got File");
            setFretSong(new File(intent.getData().getPath()));
        }
        else{
            Log.d(TAG, "CONFIG CHANGE");
            getSupportActionBar().setTitle(""); //fragment.getFretSong().getName());
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
        if (id == android.R.id.home) {
            onBackPressed();
        }
        else if(id == R.id.action_edit) {
            showFretEdit();
            return true;
        }
        else if(id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showFretEdit() {
        Intent intent = new Intent(this, FretSongEditActivity.class);
        // Add the file location into the intent, so that the editor can update the file
        Log.d(TAG, "setting data: " + getIntent().getDataString());
        intent.setData(getIntent().getData());

        try {
            startActivityForResult(intent, REQUEST_EDIT_FRET);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretSongEditActivity");
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
        if (requestCode == REQUEST_EDIT_FRET && resultCode == FretSongEditActivity.RESULT_EDITED) {
            Log.d(TAG, "HELLO EDIT_FRET Finished");
            // Reload the fretTrack because it has been edited
            setFretSong(new File(getIntent().getData().getPath()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setFretSong(File file) {
        progressBar.setVisibility(View.VISIBLE);
        SongLoaderTask songLoaderTask = new SongLoaderTask(file);
        songLoaderTask.setSongLoadedListener(new SongLoaderTask.SongLoadedListener() {
            @Override
            public void OnFileLoaded(FretSong fretSong) {
                fragment.setFretSong(fretSong);
                getSupportActionBar().setTitle("");
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void OnError(String msg) {
                Toast.makeText(FretViewActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        songLoaderTask.execute();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed");
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }
}
