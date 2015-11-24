package com.bondevans.fretboard.fretlist;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.bondevans.fretboard.firebase.FirebaseListAdapter;
import com.bondevans.fretboard.firebase.dao.Song;
import com.bondevans.fretboard.R;
import com.firebase.client.Query;

/**
 * @author Paul evans
 * @since 23/11/15
 *
 */
public class FretListAdapter extends FirebaseListAdapter<Song> {

    public FretListAdapter(Query ref, Activity activity, int layout) {
        super(ref, Song.class, layout, activity);
    }

    /**
     * Bind an instance of the <code>Song</code> class to our view. This method is called by <code>FirebaseListAdapter</code>
     * when there is a description change, and we are given an instance of a View that corresponds to the layout that we passed
     * to the constructor, as well as a single <code>Song</code> instance that represents the current description to bind.
     *
     * @param view A view instance corresponding to the layout we passed to the constructor.
     * @param song An instance representing the current state of a song message
     */
    @Override
    protected void populateView(View view, Song song) {
        // Map a Song object to an entry in our listview
        TextView nameText = (TextView) view.findViewById(R.id.name);
        nameText.setText(song.getName());
    }
}
