package com.bondevans.frets.fretlist;

import android.view.View;
import android.widget.TextView;

import com.bondevans.frets.R;
import com.bondevans.frets.firebase.dao.Fret;
import com.bondevans.frets.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

class FretHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
    private static final String TAG = FretHolder.class.getSimpleName();
    //Define all of your views here
    private final TextView mName;
    private final TextView mDescription;
    private final FretRecyclerViewClickListener mListener;

    //Define a constructor taking a View as its parameter
    public FretHolder(@NonNull View itemView, FretRecyclerViewClickListener listener) {
        super(itemView);
        Log.d(TAG, "HELLO");
        //Remembered we defined an id attribute to our TextView in fretlist_item.xml
        mName = itemView.findViewById(R.id.name);
        mDescription = itemView.findViewById(R.id.description);
        // Click Listener
        mListener = listener;
        itemView.setOnClickListener(this);
    }

    public void bind(@NonNull Fret fret) {
        Log.d(TAG, "HELLO: " + fret.getName());
        setName(fret.getName());
        setDescription(fret.getDescription());
    }

    private void setName(@Nullable String name) {
        mName.setText(name);
    }
    private void setDescription(@Nullable String text) {
        mDescription.setText(text);
    }

    @Override
    public void onClick(View v) {
        mListener.onClick(v, getAdapterPosition());
    }
}