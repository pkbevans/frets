package com.bondevans.frets.fretlist;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.firebase.dao.Fret;
import com.bondevans.frets.firebase.dao.SongContents;
import com.bondevans.frets.fretviewer.FretViewActivity;
import com.bondevans.frets.utils.FileWriterTask;
import com.bondevans.frets.utils.Log;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A fragment representing a list of Items.
 */
public class FretFragment extends Fragment {
    private static final String TAG = FretFragment.class.getSimpleName();
    private static final String ARG_COLUMN_COUNT = "column-count";
    FretRecyclerViewAdapter mAdapter;
    FretApplication mApp;
    DatabaseReference mDbRef;
    private ProgressDialog progressDialog;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FretFragment() {
    }

    @SuppressWarnings("unused")
    public static FretFragment newInstance(int columnCount) {
        FretFragment fragment = new FretFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate");
        mApp = (FretApplication)getActivity().getApplicationContext();
        if (getArguments() != null) {
            int columnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
        // Setup the progress dialog that is displayed later
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fret_layout, container, false);

        mDbRef = FirebaseDatabase.getInstance().getReference();
        Query query = mDbRef.child("users").child(mApp.getUID()).child("frets");
        Log.d(TAG, "HELLO onCreateView");

        FirebaseRecyclerOptions<Fret> options =
                new FirebaseRecyclerOptions.Builder<Fret>()
                        .setQuery(query, Fret.class)
                        .build();

        FretRecyclerViewClickListener listener = new FretRecyclerViewClickListener() {
            @Override
            public void onClick(View v, int position) {
                // Show progress bar
                progressDialog.show();
                Fret fret = (Fret) mAdapter.getItem(position);
                Log.d(TAG, "onListItemClick: " + fret.getId());
                // See if we've got this song in the cache
                final File cacheFile = new File(getActivity().getExternalFilesDir(null), fret.getId() + ".xml");
                if (cacheFile.exists()) {
                    // Always open FretViewer by passing file reference
                    showFretView(cacheFile);
                } else {
                    Log.d(TAG, "NOT in cache: " + cacheFile.getName());
                    // Get the SongContent from the server
                    DatabaseReference songRef = FirebaseDatabase.getInstance().getReference().child(SongContents.childName).child(fret.getId());
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
                            Toast.makeText(FretFragment.this.getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                // Write out a click on this song
                FretApplication app = (FretApplication) getActivity().getApplicationContext();
                FBWrite.usage(mDbRef.getRoot(), app.getUID(), fret.getId());

            }
        };

        mAdapter = new FretRecyclerViewAdapter(options, listener);
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(mAdapter);
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "HELLO onStart");
        //  Start the adapter
        mAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "HELLO onStop");
        mAdapter.stopListening();
    }

    /**
     * Launches the FretViewer Activity
     * @param cacheFile Cached file with Fret contents
     */
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

    /**
     * Delete a local fret
     * @param fretId Fret ID
     */
    @SuppressWarnings("unused")
    private void deletePrivateFret(String fretId) {
        FretApplication app = (FretApplication)getActivity().getApplicationContext();

        FBWrite.deletePrivateSong(FirebaseDatabase.getInstance().getReference(),app.getUID(),fretId );
    }
}