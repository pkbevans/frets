package com.bondevans.fretboard.filebrowser;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.firebase.FBWrite;
import com.bondevans.fretboard.fretview.FretSong;
import com.bondevans.fretboard.fretview.FretSongLoader;
import com.bondevans.fretboard.fretviewer.FretViewActivity;
import com.bondevans.fretboard.midi.MidiImporter;
import com.bondevans.fretboard.utils.FileLoaderTask;
import com.bondevans.fretboard.utils.Log;
import com.firebase.client.Firebase;

import java.io.File;

public class FileBrowserActivity extends Activity implements
        FileBrowserFragment.OnFileSelectedListener {
    private static final String TAG = "FileBrowserActivity";
    private static final int REFRESH_ID = Menu.FIRST + 16;
    private static final int UP_ID = Menu.FIRST + 17;
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4523;
    private static final String TAG_SONGDETAILS = "SongDets";
    private Firebase mFirebaseRef;
    private FileBrowserFragment fileBrowserFragment = null;
    private boolean mUpEnabled = false;
    private Menu mMenu = null;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate");
        mFirebaseRef = new Firebase(getString(R.string.firebase_url));
        checkFileAccessPermission();
        setContentView(R.layout.filebrowser_activity);// This is the xml with all the different frags
        getActionBar();
        FragmentManager fm = getFragmentManager();
        fileBrowserFragment = (FileBrowserFragment) fm.findFragmentById(R.id.browser_fragment);
        // Setup the progress dialog that is displayed later
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(false);
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onCreateOptionsMenu(com.actionbarsherlock.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;

        if (mUpEnabled) {
            menu.add(0, UP_ID, 1, getString(R.string.up))
                    .setIcon(R.drawable.up_button)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        menu.add(0, REFRESH_ID, 0, getString(R.string.refresh))
                .setIcon(R.drawable.ic_reload)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
                // Back to Songs
                finish();
                break;
            case REFRESH_ID:
                fileBrowserFragment.refresh();
                break;
            case UP_ID:
                upOneLevel(null);
                break;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "HELLO on SaveInstanceState");

        super.onSaveInstanceState(outState);
    }

    public void onFileSelected(File file) {
        if (file.getName().endsWith("xml")) {
            progressDialog.show();
            // Frig to allow cache files to be opened
            // Get the file
            Log.d(TAG, "Got file in cache: " + file.getName());
            FileLoaderTask fileLoader = new FileLoaderTask(file);
            fileLoader.setFileLoadedListener(new FileLoaderTask.FileLoadedListener() {
                @Override
                public void OnFileLoaded(String contents) {
                    progressDialog.hide();
                    Log.d(TAG, "File loaded");
                    showFretView(contents);
                }

                @Override
                public void OnError(String msg) {
                    progressDialog.hide();
                    Toast.makeText(FileBrowserActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
            fileLoader.execute();
        } else if (file.getName().endsWith("mid")) {
            // Import the midi file into an instance of FretSong
            // write out to file in cache (/sdcard/android.com.bondevans.fretplayer....)
            MidiImporter midiImporter = new MidiImporter(file,
                    new File(getExternalFilesDir(null), file.getName() + ".xml"));
            midiImporter.setFileImportedListener(new MidiImporter.FileImportedListener() {
                @Override
                public void OnImportedLoaded(final File file) {
                    // Get the Song name and description
                    SongDetailsDialog songDetailsDialog = new SongDetailsDialog();
                    songDetailsDialog.setSongDetailsListener(new SongDetailsDialog.SongDetailsListener() {
                        @Override
                        public void OnLoginDetailsEntered(String name, String description) {
                            // Write to server
                            writeSongToServer(file, name, description);
                        }

                        @Override
                        public void OnCancel() {
                            Toast.makeText(FileBrowserActivity.this, "Imported cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });
                    songDetailsDialog.show(getFragmentManager(), TAG_SONGDETAILS);
                }

                @Override
                public void OnError(String msg) {
                    Toast.makeText(FileBrowserActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
            midiImporter.execute();
        }
        else{
            Toast.makeText(FileBrowserActivity.this, R.string.invalid_file_type, Toast.LENGTH_SHORT).show();
        }
    }

    private void showFretView(String songContents) {
        // Open the file with the FretViewActivity
        Intent intent = new Intent(this, FretViewActivity.class);
        intent.putExtra(FretViewActivity.INTENT_SONGCONTENTS, songContents);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretViewActivity");
        }
    }
    private void writeSongToServer(final File file, final String name, final String description) {
        FretSongLoader fretSongLoader = new FretSongLoader(file);
        fretSongLoader.setSongLoadedListener(new FretSongLoader.SongLoadedListener() {
            @Override
            public void OnSongLoaded(FretSong fretSong) {
                fretSong.setName(name);
                FBWrite.addSong(mFirebaseRef, fretSong, description);
                Toast.makeText(FileBrowserActivity.this, "Imported to file:" + file.getName(), Toast.LENGTH_SHORT).show();
                finish();   //Lets get outta here
            }

            @Override
            public void OnError(String msg) {
                Toast.makeText(FileBrowserActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        fretSongLoader.execute();
    }

    @Override
    public void upOneLevel(View v) {
        fileBrowserFragment.upOneLevel();
    }

    @Override
    public void enableUp(boolean enable) {
        if (mMenu != null) {
            // If not already enabled, enable the UP button
            if (enable && !mUpEnabled) {
                mMenu.add(0, UP_ID, 1, getString(R.string.up))
                        .setIcon(R.drawable.up_button)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            //Remove the button if we are disabling it
            else if (!enable) {
                mMenu.removeItem(UP_ID);
            }
        }
        mUpEnabled = enable;
    }

    private void checkFileAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.util.Log.d(TAG, "checkFileAccessPermission 1");
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d(TAG, "checkFileAccessPermission 2");
                // Need to request permission from the user
                String[] perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(perms, REQUEST_CODE_READ_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // TODO need to handle user not allowing access.
        Log.d(TAG, "onRequestPermissionsResult");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}