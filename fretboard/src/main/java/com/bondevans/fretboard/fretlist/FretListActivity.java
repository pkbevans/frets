package com.bondevans.fretboard.fretlist;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.filebrowser.FileBrowserActivity;
import com.bondevans.fretboard.firebase.dao.SongContents;
import com.bondevans.fretboard.firebase.dao.Songs;
import com.bondevans.fretboard.player.FretViewActivity;
import com.bondevans.fretboard.utils.FileWriterTask;
import com.bondevans.fretboard.utils.Log;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.File;

public class FretListActivity extends ListActivity {
    private static final String TAG = FretListActivity.class.getSimpleName();
    private static final int BROWSER_ID = Menu.FIRST;
    private Firebase mFirebaseRef;
    private ValueEventListener mConnectedListener;
    private FretListAdapter mFretListAdapter;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fretlist_activity_main);

        // Setup our Firebase mFirebaseRef
        mFirebaseRef = new Firebase(getString(R.string.firebase_url)).child("songs");
        // TODO Need to get/set up user details
        // Setup the progress dialog that is displayed later
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, BROWSER_ID, 0, getString(R.string.import_midi))
//                .setIcon(R.drawable.ai_refresh)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        boolean handled=false;
        switch (item.getItemId()) {
            case BROWSER_ID:
                launchBrowser();
                handled=true;
                break;
        }
        return handled;
    }

    private void launchBrowser() {
        Intent intent = new Intent(this, FileBrowserActivity.class);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FileBrowserActivity");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as description changes
        final ListView listView = getListView();
        // Tell our list adapter that we only want 50 messages at a time
        mFretListAdapter = new FretListAdapter(mFirebaseRef, this, R.layout.fretlist_item);
        listView.setAdapter(mFretListAdapter);
        mFretListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mFretListAdapter.getCount() - 1);
            }
        });

        // Finally, a little indication of connection status
        mConnectedListener = mFirebaseRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Toast.makeText(FretListActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FretListActivity.this, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseRef.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
        mFretListAdapter.cleanup();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Songs song = (Songs) getListView().getItemAtPosition(position);
        Log.d(TAG, "onListItemClick: " + song.getId());
        // See if we've got this song in the cache
        final File cacheFile = new File(getExternalFilesDir(null),song.getId());
        if(cacheFile.exists()){
            // Get the file
            Log.d(TAG, "Got file in cache: " + cacheFile.getName());
            showFretView(cacheFile);
        }
        else {
            Log.d(TAG, "NOT in cache: " + cacheFile.getName());
            // Show progress bar
            progressDialog.show();
            // Get the SongContent from the server
            Firebase songRef = new Firebase(getString(R.string.firebase_url)).child("songcontents").child(song.getId());
            songRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
//                    Log.d(TAG, "CHILD:" + dataSnapshot.toString());
                    SongContents songContents = dataSnapshot.getValue(SongContents.class);
                    // Write out to cache
                    FileWriterTask fileWriterTask = new FileWriterTask(cacheFile, songContents.getContents());
                    fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
                        @Override
                        public void OnFileWritten() {
                            // Start FretViewer
                            progressDialog.hide();
                            showFretView(cacheFile);
                        }

                        @Override
                        public void OnError(String msg) {
                            progressDialog.hide();
                            Toast.makeText(FretListActivity.this,msg,Toast.LENGTH_SHORT).show();
                        }
                    });
                    fileWriterTask.execute();
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    progressDialog.hide();
                    Log.d(TAG, "OOPS " + firebaseError.getMessage());
                    Toast.makeText(FretListActivity.this, firebaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void showFretView(File file){
//        FBWrite.usage(mFirebaseRef, mUid, Usage.FEATURE_BROWSETO_FILE);
        Intent intent = new Intent(this, FretViewActivity.class);
        intent.setData(Uri.fromFile(file));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretViewActivity");
        }
    }
}
