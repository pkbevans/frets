package com.bondevans.frets.fretlist;

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

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.firebase.dao.SongContents;
import com.bondevans.frets.firebase.dao.Songs;
import com.bondevans.frets.fretviewer.FretViewActivity;
import com.bondevans.frets.utils.FileWriterTask;
import com.bondevans.frets.utils.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.ViewModelProviders;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

public class FretListFragment extends ListFragment {
    private static final String TAG = FretListFragment.class.getSimpleName();
    public static final int FRETLIST_TYPE_NONE = 0;
    public static final int FRETLIST_TYPE_PUBLIC = 1;
    public static final int FRETLIST_TYPE_PRIVATE = 2;
    private DatabaseReference mFirebaseRef;
    private FretListAdapter mFretListAdapter;
    private ProgressDialog progressDialog;
    private PageViewModel pageViewModel;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_SECTION_TYPE = "section_number";
    private int listType=FRETLIST_TYPE_NONE;

    public static FretListFragment newInstance(int index, int type) {
        FretListFragment fragment = new FretListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        bundle.putInt(ARG_SECTION_TYPE, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
            listType = getArguments().getInt(ARG_SECTION_TYPE);
        }
        pageViewModel.setIndex(index);
        // Setup our Firebase
        if(listType == FRETLIST_TYPE_PUBLIC) {
            mFirebaseRef = FirebaseDatabase.getInstance().getReference().child("songs");
        }else{
            FretApplication app = (FretApplication)getActivity().getApplicationContext();
            mFirebaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(app.getUID()).child("songs");
        }
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
            DatabaseReference songRef = FirebaseDatabase.getInstance().getReference().child(SongContents.childName).child(song.getId());
            songRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String songContents = dataSnapshot.child("contents").toString();
                    Log.d(TAG, "HELLO CHILD:" + songContents);
                    // Write out to cache and then show FretView
                    writeFileToCache(cacheFile, songContents, true);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    progressDialog.hide();
                    Log.d(TAG, "OOPS " + databaseError.getMessage());
                    Toast.makeText(FretListFragment.this.getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
