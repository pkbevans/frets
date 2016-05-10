package com.bondevans.frets.fretlist;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.bondevans.frets.firebase.FirebaseListAdapter;
import com.bondevans.frets.firebase.dao.Songs;
import com.bondevans.frets.R;
import com.firebase.client.Query;

/**
 * @author Paul evans
 * @since 23/11/15
 *
 */
public class FretListAdapter extends FirebaseListAdapter<Songs> {

    public FretListAdapter(Query ref, Activity activity, int layout) {
        super(ref, Songs.class, layout, activity);
    }

    /**
     * Bind an instance of the <code>Songs</code> class to our view. This method is called by <code>FirebaseListAdapter</code>
     * when there is a description change, and we are given an instance of a View that corresponds to the layout that we passed
     * to the constructor, as well as a single <code>Songs</code> instance that represents the current description to bind.
     *
     * @param view A view instance corresponding to the layout we passed to the constructor.
     * @param songs An instance representing the current state of a songs message
     */
    @Override
    protected void populateView(View view, Songs songs) {
        // Map a Songs object to an entry in our listview
        TextView nameText = (TextView) view.findViewById(R.id.name);
        nameText.setText(songs.getName());
        TextView descText = (TextView) view.findViewById(R.id.description);
        descText.setText(songs.getDescription());
    }
}
