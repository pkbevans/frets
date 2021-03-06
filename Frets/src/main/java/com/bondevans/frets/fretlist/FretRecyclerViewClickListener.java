package com.bondevans.frets.fretlist;

import android.view.View;

public interface FretRecyclerViewClickListener {
    void onClick(View view, int position);
    void onThumbnailClick(View view, int position);
    void onLongClick(View view,int position);
    void onDataChanged();
}
