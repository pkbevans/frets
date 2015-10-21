package com.bondevans.fretboard.fretboardplayer;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;

public class FileBrowserActivity extends Activity implements
        FileBrowserFragment.OnFileSelectedListener {
    private static final String TAG = "SongBrowserActivity";
    private static final int REFRESH_ID = Menu.FIRST + 16;
    private static final int UP_ID = Menu.FIRST + 17;
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4523;

    private FileBrowserFragment fileBrowserFragment = null;
    private boolean mUpEnabled = false;
    private Menu mMenu = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate");
        checkFileAccessPermission();
        setContentView(R.layout.file_browser_activity_main);// This is the xml with all the different frags
        ActionBar x = getActionBar();
        if(x==null)Log.d(TAG, "HELLO NO ACTION BAR");
        FragmentManager fm = getFragmentManager();
        fileBrowserFragment = (FileBrowserFragment) fm.findFragmentById(R.id.browser_fragment);
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onCreateOptionsMenu(com.actionbarsherlock.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;

        if (mUpEnabled) {
            menu.add(0, UP_ID, 1, getString(R.string.up))
                    .setIcon(R.drawable.ic_up_sel)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        menu.add(0, REFRESH_ID, 0, getString(R.string.refresh))
//                .setIcon(R.drawable.ai_refresh)
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

    @Override
    /*
	 * Handle non-menu keyboard events
	 */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "HELLO - onKeyDown");
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Log.d(TAG, "HELLO - BACK PRESSED");
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void onFileSelected(boolean inSet, File midiFile) {
        // Open the file with the SongViewerActivity
        Intent intent = new Intent(this, FretboardActivity.class);

        intent.setData(Uri.fromFile(midiFile));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretboardActivity");
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
                        .setIcon(R.drawable.ic_up_sel)
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
        android.util.Log.d(TAG, "checkFileAccessPermission 1");
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            android.util.Log.d(TAG, "checkFileAccessPermission 2");
            // Need to request permission from the user
            String [] perms = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(perms, REQUEST_CODE_READ_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // TODO need to handle user not allowing access.
        Log.d(TAG, "onRequestPermissionsResult");
    }

}