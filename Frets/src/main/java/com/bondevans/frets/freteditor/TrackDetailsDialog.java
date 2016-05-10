package com.bondevans.frets.freteditor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.bondevans.frets.R;
import com.bondevans.frets.utils.Log;

public class TrackDetailsDialog extends DialogFragment {

    public static final String TAG = TrackDetailsDialog.class.getSimpleName();
    private static final String KEY_NAME = "na";
    private TrackDetailsListener trackDetailsListener;

    public static TrackDetailsDialog newInstance(String name) {
        TrackDetailsDialog frag = new TrackDetailsDialog();
        Bundle args = new Bundle();
        args.putString(KEY_NAME, name);
        frag.setArguments(args);
        return frag;
    }

    public interface TrackDetailsListener {
        void OnUpdate(String name);
    }

    public void setTrackDetailsListener(TrackDetailsListener trackDetailsListener) {
        this.trackDetailsListener = trackDetailsListener;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle args) {
        final EditText name;

        View layout = View.inflate(getActivity(), R.layout.track_details_dialog, null);

        if (args != null) {
            Log.d(TAG, "ARGS NOT NULL!!!");
        }
        name = (EditText) layout.findViewById(R.id.name);
        name.setText(getArguments().getString(KEY_NAME));

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.track_details)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        trackDetailsListener.OnUpdate(name.getText().toString().trim());
                    }
                })
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }
}
