package com.bondevans.frets.fretviewer;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
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
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.freteditor.FretSongEditActivity;
import com.bondevans.frets.fretview.FretSong;
import com.bondevans.frets.utils.FretLoaderTask;
import com.bondevans.frets.utils.Synth;

import java.io.File;

public class FretViewActivity extends AppCompatActivity {

    private static final String TAG = FretViewActivity.class.getSimpleName();
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4522;
    private static final int REQUEST_EDIT_FRET = 678;
    private FretViewFragment fragment;
    private ProgressBar progressBar;
    private MidiManager mMidiManager;
    MidiDeviceInfo mMidiDeviceInfo;
    MidiDeviceInfo.PortInfo mPortInfo;
    private MidiReceiver mMidiReceiver;
    private MidiDevice mOpenDevice;
    private FretApplication mApp;
    private FretSong mFretSong;
    private boolean loaded=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFileAccessPermission();
        setContentView(R.layout.fretview_activity);
        fragment = (FretViewFragment) getFragmentManager().findFragmentById(R.id.fragment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        progressBar = findViewById(R.id.progress_bar); // Attaching the layout to the toolbar object
        // Get the midi port from the Application
        setupMidi();
        mApp = (FretApplication)getApplicationContext();
        mMidiDeviceInfo = mApp.getMidiDeviceInfo();
        mPortInfo = mApp.getPortInfo();
        openSynth(mMidiDeviceInfo, mPortInfo);

        if (savedInstanceState == null) {
            //  We should have the song contents in the intent
            Intent intent = getIntent();
            Log.d(TAG, "Got File");
            setFretSong(new File(intent.getData().getPath()));
        }
        else{
            Log.d(TAG, "CONFIG CHANGE");
            getSupportActionBar().setTitle(fragment.getFretSong().getName());
        }
    }
    private void setupMidi() {
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);
        if (mMidiManager == null) {
            Toast.makeText(FretViewActivity.this, "MidiManager is null!", Toast.LENGTH_LONG)
                    .show();
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
//        else if(id == R.id.action_edit) {
//            showFretEdit();
//            return true;
//        }
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
        FretLoaderTask songLoaderTask = new FretLoaderTask(file);
        songLoaderTask.setFretLoadedListener(new FretLoaderTask.FretLoadedListener() {
            @Override
            public void OnFileLoaded(FretSong fretSong) {
                if (loaded){
                    // Synth is also loaded so we are good to go
                    fragment.setFretSong(mApp, mMidiReceiver, fretSong);
                    getSupportActionBar().setTitle(fretSong.getName());
                    progressBar.setVisibility(View.INVISIBLE);
                }else{
                    loaded=true;
                    mFretSong = fretSong;
                }
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
    private void openSynth(MidiDeviceInfo midiDeviceInfo, final MidiDeviceInfo.PortInfo portInfo) {
        Log.d(TAG, "HELLO openSynth");
        final MidiDeviceInfo info = midiDeviceInfo;
        mMidiManager.openDevice(info, new MidiManager.OnDeviceOpenedListener() {
            @Override
            public void onDeviceOpened(MidiDevice device) {
                if (device == null) {
                    Log.e(TAG, "HELLO could not open " + info);
                } else {
                    mOpenDevice = device;
                    MidiInputPort inputPort = mOpenDevice.openInputPort(
                            portInfo.getPortNumber());
                    if (null == inputPort) {
                        mMidiReceiver = null;
                        Toast.makeText(FretViewActivity.this, getString(R.string.synth_error)+info, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "HELLO could not open input port: " + info);
                        // TODO - need to display error to user
                    }else{
                        Log.d(TAG, "HELLO Port opened " + info);
                        mMidiReceiver = inputPort;
                        if (loaded){
                            // FretSong has also been loaded so we are good to go
                            fragment.setFretSong(mApp, mMidiReceiver, mFretSong);
                            progressBar.setVisibility(View.INVISIBLE);
                        }else{
                            loaded=true;
                        }
                        Log.d(TAG, "HELLO maxMessageSize=" + mMidiReceiver.getMaxMessageSize());
                    }
                }
            }
        }, null);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        Synth.closeSynth(mOpenDevice);
    }

}
