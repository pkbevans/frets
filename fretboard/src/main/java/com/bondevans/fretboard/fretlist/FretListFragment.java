package com.bondevans.fretboard.fretlist;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.app.FretApplication;
import com.bondevans.fretboard.firebase.FBWrite;
import com.bondevans.fretboard.firebase.dao.SongContents;
import com.bondevans.fretboard.firebase.dao.Songs;
import com.bondevans.fretboard.fretviewer.FretViewActivity;
import com.bondevans.fretboard.utils.FileWriterTask;
import com.bondevans.fretboard.utils.Log;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.File;

public class FretListFragment extends ListFragment {
    private static final String TAG = FretListFragment.class.getSimpleName();
    private Firebase mFirebaseRef;
    private FretListAdapter mFretListAdapter;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup our Firebase
        mFirebaseRef = new Firebase(getString(R.string.firebase_url)).child("songs");
        // Setup the progress dialog that is displayed later
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(false);
    }

    /* (non-Javadoc)
 * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
 */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fretlist_layout, container, false);
        Log.d(TAG, "HELLO onCreatView");
        return contentView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as description changes
        final ListView listView = getListView();
        // Tell our list adapter that we only want 50 messages at a time
        mFretListAdapter = new FretListAdapter(mFirebaseRef, getActivity(), R.layout.fretlist_item);
        listView.setAdapter(mFretListAdapter);
        mFretListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mFretListAdapter.getCount() - 1);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        progressDialog.dismiss();
        mFretListAdapter.cleanup();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Show progress bar
        progressDialog.show();
        Songs song = (Songs) getListView().getItemAtPosition(position);
        Log.d(TAG, "onListItemClick: " + song.getId());
        // See if we've got this song in the cache
        final File cacheFile = new File(getActivity().getExternalFilesDir(null), song.getId() + ".xml");
        if (cacheFile.exists()) {
            // Always open FretViewer by passing file reference
            showFretView(cacheFile);
        } else {
            Log.d(TAG, "NOT in cache: " + cacheFile.getName());
            // Get the SongContent from the server
            Firebase songRef = new Firebase(getString(R.string.firebase_url)).child(SongContents.childName).child(song.getId());
            songRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String songContents = dataSnapshot.child("contents").toString();
                    Log.d(TAG, "HELLO CHILD:" + songContents);
                    // Write out to cache and then show FretView
                    writeFileToCache(cacheFile, songContents, true);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    progressDialog.hide();
                    Log.d(TAG, "OOPS " + firebaseError.getMessage());
                    Toast.makeText(FretListFragment.this.getActivity(), firebaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        // Write out a click on this song
        FretApplication app = (FretApplication) getActivity().getApplicationContext();
        FBWrite.usage(mFirebaseRef.getRoot(), app.getUID(), song.getId());
    }

    private void showFretView(File cacheFile) {
        progressDialog.hide();
        Intent intent = new Intent(getActivity(), FretViewActivity.class);
        intent.setData(Uri.fromFile(cacheFile));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretViewActivity");
        }
    }

    private void writeFileToCache(final File cacheFile, String songContents, final boolean showFretView) {
        FileWriterTask fileWriterTask = new FileWriterTask(cacheFile, songContents);
        fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
            @Override
            public void OnFileWritten() {
                Log.d(TAG, "File written to cache: " + cacheFile.getName());
                if (showFretView) {
                    showFretView(cacheFile);
                }
            }

            @Override
            public void OnError(String msg) {
                progressDialog.hide();
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        });
        fileWriterTask.execute();
    }
}
