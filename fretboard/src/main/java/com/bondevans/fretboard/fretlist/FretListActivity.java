package com.bondevans.fretboard.fretlist;

import android.app.ListActivity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.bondevans.fretboard.firebase.dao.Song;
import com.bondevans.fretboard.R;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class FretListActivity extends ListActivity {
    private static final String FIREBASE_URL = "https://fretboardplayer.firebaseio.com";
    private Firebase mFirebaseRef;
    private ValueEventListener mConnectedListener;
    private FretListAdapter mFretListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fretlist_activity_main);

        // Setup our Firebase mFirebaseRef
        mFirebaseRef = new Firebase(FIREBASE_URL).child("songs");
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
        Song selectedSong = (Song) getListView().getItemAtPosition(position);
        // TODO Load up the song into FretView
    }
}
