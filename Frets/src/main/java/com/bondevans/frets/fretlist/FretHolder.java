package com.bondevans.frets.fretlist;

import android.view.View;
import android.widget.TextView;

import com.bondevans.frets.R;
import com.bondevans.frets.firebase.dao.Fret;
import com.bondevans.frets.utils.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

class FretHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
    private static final String TAG = FretHolder.class.getSimpleName();
    //Define all of your views here
    private final TextView mName;
    private final TextView mDescription;
    private final TextView mUser;
    private final TextView mInstrument;
    private final TextView mDateCreated;
    private final FretRecyclerViewClickListener mListener;
    final DateFormat simpleDF = new SimpleDateFormat("dd MMM yyyy");

    //Define a constructor taking a View as its parameter
    public FretHolder(@NonNull View itemView, FretRecyclerViewClickListener listener) {
        super(itemView);
        Log.d(TAG, "HELLO");
        //Remembered we defined an id attribute to our TextView in fretlist_item.xml
        mName = itemView.findViewById(R.id.name);
        mDescription = itemView.findViewById(R.id.description);
        mUser = itemView.findViewById(R.id.user_name);
        mInstrument = itemView.findViewById(R.id.instrument);
        mDateCreated = itemView.findViewById(R.id.date_created);
        // Click Listener
        mListener = listener;
        itemView.setOnClickListener(this);
    }

    public void bind(@NonNull Fret fret) {
        Log.d(TAG, "HELLO: " + fret.getName());
        setName(fret.getName());
        setDescription(fret.getDescription());
        setUser(fret.getUserId());
        setInstrument(fret.getInstrumentName(fret.getInstrument()));
        String d = formatDate(fret.getDatePublished());
        Log.d(TAG, "date:"+d);
        setDateCreated(formatDate(fret.getDatePublished()));
    }

    private String formatDate(long createDate){
        Date now = new Date();
        long nowL = now.getTime();
        long diff = nowL - createDate;
        long diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

        if(diffDays>365){
            // Over a year ago
            return "Ages ago...";
        }else if(diffDays>31){
            return simpleDF.format(new Date(createDate));
        }else if(diffDays>1){
            return diffDays+" days ago";
        }else if(diffDays == 1){
            return "1 day ago";
        }else{
            return "NEW!!";
        }
    }
    private void setName(@Nullable String name) {
        mName.setText(name);
    }
    private void setDescription(@Nullable String text) {
        mDescription.setText(text);
    }
    private void setUser(@Nullable String text) {
        mUser.setText(text);
    }
    private void setInstrument(@Nullable String text) {
        mInstrument.setText(text);
    }
    private void setDateCreated(@Nullable String text) {
        mDateCreated.setText(text);
    }

    @Override
    public void onClick(View v) {
        mListener.onClick(v, getAdapterPosition());
    }
}