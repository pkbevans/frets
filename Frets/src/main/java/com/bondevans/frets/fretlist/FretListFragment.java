package com.bondevans.frets.fretlist;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.firebase.dao.Fret;
import com.bondevans.frets.firebase.dao.FretContents;
import com.bondevans.frets.firebase.dao.UserProfile;
import com.bondevans.frets.fretviewer.FretViewActivity;
import com.bondevans.frets.user.UserProfileActivity;
import com.bondevans.frets.user.UserProfileFragment;
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
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.app.usage.UsageEvents.Event.NONE;

/**
 * A fragment representing a list of Items.
 */
public class FretListFragment extends Fragment implements FretListActivity.QueryUpdateListener {
    private static final String TAG = FretListFragment.class.getSimpleName();
    private static final String ARG_COLUMN_COUNT = "column-count";
    FretRecyclerViewAdapter mAdapter;
    FretApplication mApp;
    DatabaseReference mDbRef;
    private ProgressDialog progressDialog;
    private FretListViewModel fretListViewModel;
    private int mListType;
    private Query mQuery;
    private ArrayList<Item> mSelectedItems = new ArrayList<>();
    private FirebaseRecyclerOptions<Fret> mOptions;
    private FretRecyclerViewClickListener mListener;
    private RecyclerView mRecyclerView;
    private TextView mEmptyText;

    private ActionMode.Callback mActionModeCallbacks = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(NONE, 1, 1, "Publish");
            menu.add(NONE, 2, 2, "Delete");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.d(TAG, "HELLO item:"+item.toString());
            switch(item.getItemId()){
                case 1:
                    //Publish
                    for(Item i : mSelectedItems) {
                        publishPrivateFret(i.ref, i.position);
                    }
                    break;
                case 2:
                    // Delete
                    for(Item i : mSelectedItems) {
                        deletePrivateFret(i.ref);
                    }
                    break;
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Make sure all items are UN-highlighted
            for(Item i : mSelectedItems) {
                View view = mRecyclerView.getLayoutManager().findViewByPosition(i.position);
                view.setBackgroundColor(Color.WHITE);
            }
            mSelectedItems.clear();
            mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onQueryUpdate(String search) {
        Log.d(TAG, "HELLO OnQueryUpdate: "+search);

        Query query = mQuery.startAt(search.toUpperCase()).endAt(search.toUpperCase() + "\uf8ff");
        FirebaseRecyclerOptions<Fret> options = new FirebaseRecyclerOptions.Builder<Fret>()
                .setQuery(query, Fret.class)
                .build();

        mAdapter.updateOptions(options);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        ((FretListActivity) activity).registerDataUpdateListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((FretListActivity) getActivity()).unregisterDataUpdateListener(this);
    }

    private class Item {
        String ref;
        int position;

        public Item(String ref, int position) {
            this.ref = ref;
            this.position = position;
        }
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FretListFragment() {
    }

    @SuppressWarnings("unused")
    public static FretListFragment newInstance(int index, int type) {
        FretListFragment fragment = new FretListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(FretListActivity.ARG_FRETLIST_NUMBER, index);
        bundle.putInt(FretListActivity.ARG_FRETLIST_TYPE, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate");
        fretListViewModel = new ViewModelProvider(this).get(FretListViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(FretListActivity.ARG_FRETLIST_NUMBER);
            mListType = getArguments().getInt(FretListActivity.ARG_FRETLIST_TYPE);
        }
        fretListViewModel.setIndex(index);
        mApp = (FretApplication)getActivity().getApplicationContext();
        mDbRef = FirebaseDatabase.getInstance().getReference();
        if(mListType == FretListActivity.FRETLIST_TYPE_PUBLIC) {
            Log.d(TAG, "HELLO onCreate PUBLIC");
            mQuery = mDbRef.child("frets").orderByChild("name");
        }else{
            Log.d(TAG, "HELLO onCreate PRIVATE");
            mQuery = mDbRef.child("users").child(mApp.getUID()).child("frets").orderByChild("name");
        }

        if (getArguments() != null) {
            int columnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
        mListener = new FretRecyclerViewClickListener() {
            @Override
            public void onClick(View v, int position) {
                // If the Action bar is currently being shown, and this item is highlighted then
                // then a click just UN-highlights the item
                String fretref = mAdapter.getRef(position).getKey();
                for(Item i : mSelectedItems){
                    if(i.position == position && i.ref.contentEquals(fretref)){
                        v.setBackgroundColor(Color.WHITE);
                        mSelectedItems.remove(i);
                        return;
                    }
                }
                // Show progress bar
                progressDialog.show();
                Fret fret = mAdapter.getItem(position);
                Log.d(TAG, "onListItemClick: " + fret.getName());
                // See if we've got this song in the cache
                final File cacheFile = new File(getActivity().getExternalFilesDir(null), fret.getContentId() + ".json");
                if (cacheFile.exists()) {
                    // Always open FretViewer by passing file reference
                    Log.d(TAG, "Found in cache: " + cacheFile.getName());
                    showFretView(cacheFile);
                } else {
                    Log.d(TAG, "NOT in cache: " + cacheFile.getName());
                    // Get the SongContent from the server
                    DatabaseReference songRef = FirebaseDatabase.getInstance().getReference().child(FretContents.childName).child(fret.getContentId());
                    songRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            FretContents fretContents = dataSnapshot.getValue(FretContents.class);
                            Log.d(TAG, "HELLO CHILD:" + fretContents.getContents());
                            // Write out to cache and then show FretView
                            writeFileToCache(cacheFile, fretContents.getContents(), true);
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
                FBWrite.usage(mDbRef.getRoot(), FretApplication.getUID(), fret.getContentId());
            }

            @Override
            public void onThumbnailClick(View view, int position) {
                Fret fret = mAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), UserProfileActivity.class);
                intent.putExtra(UserProfileFragment.INTENT_UID, fret.getUserId());
                intent.putExtra(UserProfileFragment.INTENT_EDITABLE, false);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "NO ACTIVITY FOUND: "+UserProfileActivity.class.getSimpleName());
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                if(mListType == FretListActivity.FRETLIST_TYPE_PRIVATE) {
                    // Long press only applicable on Private frets
                    String fretRef = mAdapter.getRef(position).getKey();
                    Log.d(TAG, "onLongClick: " + fretRef);
                    view.setBackgroundColor(Color.GREEN);
                    ((AppCompatActivity) view.getContext()).startSupportActionMode(mActionModeCallbacks);
                    mSelectedItems.add(new Item(fretRef, position));
                }
            }

            @Override
            public void onDataChanged() {
//                Log.d(TAG, "HELLO onDataChanged: "+mListType+":" +mAdapter.getItemCount());
                if(mAdapter.getItemCount() == 0){
                    mEmptyText.setVisibility(View.VISIBLE);
                } else {
                    mEmptyText.setVisibility(View.INVISIBLE);
                }
            }
        };
        // Setup the progress dialog that is displayed later
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "HELLO onCreateView");
        View view = inflater.inflate(R.layout.fretfragment_layout, container, false);

        mOptions = new FirebaseRecyclerOptions.Builder<Fret>()
                        .setQuery(mQuery, Fret.class)
                        .build();

        mAdapter = new FretRecyclerViewAdapter(mOptions, mListener);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        mEmptyText.setVisibility(View.INVISIBLE);
        if (mRecyclerView != null) {
            Context context = view.getContext();
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            mRecyclerView.setAdapter(mAdapter);
            DividerItemDecoration did = new DividerItemDecoration(view.getContext(),
                    DividerItemDecoration.VERTICAL);
            did.setDrawable(getContext().getResources().getDrawable(R.drawable.fret));
            mRecyclerView.addItemDecoration(did);
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
//        Log.d(TAG, "HELLO onStart");
        //  Start the adapter
        mAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
//        Log.d(TAG, "HELLO onStop");
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
    private void writeFileToCache(final File cacheFile, String fretContents, final boolean showFretView) {
        FileWriterTask fileWriterTask = new FileWriterTask(cacheFile, fretContents);
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
     * @param fretRef Fret reference
     */
    private void deletePrivateFret(String fretRef) {
        Log.d(TAG, "deletePrivateFret: " + fretRef);
        FBWrite.deletePrivateFret(FirebaseDatabase.getInstance().getReference(), FretApplication.getUID(),fretRef );
    }

    /**
     * Publish a local fret
     * @param fretRef Fret reference
     * @param position position in list
     */
    private void publishPrivateFret(String fretRef, int position ) {
        Log.d(TAG, "publishPrivateFret: " + fretRef);
        Fret fret = mAdapter.getItem(position);
        FBWrite.publishPrivateFret(FirebaseDatabase.getInstance().getReference(), fret, FretApplication.getUID(),fretRef);
    }
}