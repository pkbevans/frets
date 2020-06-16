package com.bondevans.frets.fretlist;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.bondevans.frets.firebase.FirebaseListAdapter;
import com.bondevans.frets.firebase.dao.Fret;
import com.bondevans.frets.R;
import com.google.firebase.database.Query;

/**
 * @author Paul evans
 * @since 23/11/15
 *
 * TODO - Add User details, photo, date, etc
 * TODO - Add ability to like/up-vote
 *
 */
public class FretListAdapter extends FirebaseListAdapter<Fret> {

    public FretListAdapter(Query ref, Activity activity, int layout) {
        super(ref, Fret.class, layout, activity);
    }

    /**
     * Bind an instance of the <code>Fret</code> class to our view. This method is called by <code>FirebaseListAdapter</code>
     * when there is a description change, and we are given an instance of a View that corresponds to the layout that we passed
     * to the constructor, as well as a single <code>Fret</code> instance that represents the current description to bind.
     *
     * @param view A view instance corresponding to the layout we passed to the constructor.
     * @param fret An instance representing the current state of a fret
     */
    @Override
    protected void populateView(View view, Fret fret) {
        // Map a Fret object to an entry in our listview
        TextView nameText = (TextView) view.findViewById(R.id.name);
        nameText.setText(fret.getName());
        TextView descText = (TextView) view.findViewById(R.id.description);
        descText.setText(fret.getDescription());
    }
}
