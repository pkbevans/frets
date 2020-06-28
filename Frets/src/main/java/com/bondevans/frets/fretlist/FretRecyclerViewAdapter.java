package com.bondevans.frets.fretlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bondevans.frets.R;
import com.bondevans.frets.firebase.dao.Fret;
import com.bondevans.frets.utils.Log;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseError;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FretRecyclerViewAdapter extends FirebaseRecyclerAdapter<Fret, FretHolder> {
    private static final String TAG = FretRecyclerViewAdapter.class.getSimpleName();
    private FretRecyclerViewClickListener mListener;

    /**
     * Initialize a {@link RecyclerView.Adapter} that listens to a Firebase query. See
     * {@link FirebaseRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public FretRecyclerViewAdapter(@NonNull FirebaseRecyclerOptions<Fret> options) {
        super(options);
    }

    FretRecyclerViewAdapter(@NonNull FirebaseRecyclerOptions<Fret> options, FretRecyclerViewClickListener listener) {
        super(options);
        mListener = listener;
    }

    @Override
    public FretHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new instance of the ViewHolder,
        Log.d(TAG, "HELLO onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fretlist_item, parent, false);
        return new FretHolder(view, mListener);
    }
    @Override
    public void onDataChanged() {
        // Called each time there is a new data snapshot. You may want to use this method
        // to hide a loading spinner or check for the "no documents" state and update your UI.
        Log.d(TAG, "HELLO onDataChanged");
        mListener.onDataChanged();
    }

    @Override
    public void onError(DatabaseError e) {
        // Called when there is an error getting data. You may want to update
        // your UI to display an error message to the user.
        Log.d(TAG, "HELLO onError");
    }
    @Override
    protected void onBindViewHolder(FretHolder holder, int position, Fret fret) {
        String keyId = this.getRef(position).getKey();
        Log.d(TAG, "HELLO onBindViewHolder. position:"+position+ " fret: "+keyId);
        // Bind the Fret object to the FretHolder
        holder.bind(fret);
    }
}