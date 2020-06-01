package com.bondevans.frets.filebrowser;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.freteditor.FretSongEditActivity;
import com.bondevans.frets.fretviewer.FretViewActivity;
import com.bondevans.frets.midi.MidiImporter;
import com.bondevans.frets.utils.Log;

import java.io.File;

public class FileBrowserActivity extends AppCompatActivity implements
        FileBrowserFragment.OnFileSelectedListener {
    private static final String TAG = "FileBrowserActivity";
    private static final int REFRESH_ID = Menu.FIRST + 16;
    private static final int UP_ID = Menu.FIRST + 17;
    private FileBrowserFragment fileBrowserFragment = null;
    private boolean mUpEnabled = false;
    private Menu mMenu = null;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate");
        setContentView(R.layout.filebrowser_activity);// This is the xml with all the different frags
        getActionBar();
        FragmentManager fm = getSupportFragmentManager();
        fileBrowserFragment = (FileBrowserFragment) fm.findFragmentById(R.id.browser_fragment);
        // Setup the progress dialog that is displayed later
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(false);

        Toolbar toolbar = findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        getSupportActionBar().setTitle("");             // Empty string - since we are using the logo image
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onCreateOptionsMenu(com.actionbarsherlock.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;

        if (mUpEnabled) {
            menu.add(0, UP_ID, 1, getString(R.string.up))
                    .setIcon(R.drawable.ic_up_button)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        menu.add(0, REFRESH_ID, 0, getString(R.string.refresh))
                .setIcon(R.drawable.ic_refresh)
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
            showFretView(file);
            // Get the file
            Log.d(TAG, "Got file in cache: " + file.getName());
        } else if (file.getName().endsWith("mid")) {
            // Import the midi file into an instance of FretSong
            // write out to file in cache (/sdcard/android.com.bondevans.fretplayer....)
            MidiImporter midiImporter = new MidiImporter(file,
                    new File(getExternalFilesDir(null), file.getName() + ".xml"));
            midiImporter.setFileImportedListener(new MidiImporter.FileImportedListener() {
                @Override
                public void OnImportedLoaded(final File file) {
                    showFretEdit(file);
                }

                @Override
                public void OnError(String msg) {
                    Toast.makeText(FileBrowserActivity.this, getString(R.string.midi_file_error)+msg, Toast.LENGTH_LONG).show();
                }
            });
            midiImporter.execute();
        }
        else{
            Toast.makeText(FileBrowserActivity.this, R.string.invalid_file_type, Toast.LENGTH_SHORT).show();
        }
    }

    private void showFretView(File file) {
        progressDialog.hide();
        Intent intent = new Intent(this, FretViewActivity.class);
        intent.setData(Uri.fromFile(file));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretViewActivity");
        }
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
                        .setIcon(R.drawable.ic_up_button)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            //Remove the button if we are disabling it
            else if (!enable) {
                mMenu.removeItem(UP_ID);
            }
        }
        mUpEnabled = enable;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showFretEdit(File file) {
        Intent intent = new Intent(this, FretSongEditActivity.class);
        intent.setData(Uri.fromFile(file));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: "+FretSongEditActivity.class.getSimpleName());
        }
    }
}