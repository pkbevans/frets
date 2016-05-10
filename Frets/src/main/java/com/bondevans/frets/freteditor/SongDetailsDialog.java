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

public class SongDetailsDialog extends DialogFragment {

    public static final String TAG = SongDetailsDialog.class.getSimpleName();
    private static final String KEY_NAME = "na";
    private SongDetailsListener songDetailsListener;

    public static SongDetailsDialog newInstance(String name) {
        SongDetailsDialog frag = new SongDetailsDialog();
        Bundle args = new Bundle();
        args.putString(KEY_NAME, name);
        frag.setArguments(args);
        return frag;

    }
    public interface SongDetailsListener {
        void OnSongDetailsEntered(String name, String description);
        void OnCancel();
    }

    public void setSongDetailsListener(SongDetailsListener songDetailsListener) {
        this.songDetailsListener = songDetailsListener;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle args) {
        final EditText name;
        final EditText description;

        View layout = View.inflate(getActivity(), R.layout.song_details_dialog, null);

        if (args != null) {
            Log.d(TAG, "ARGS NOT NULL!!!");
        }
        name = (EditText) layout.findViewById(R.id.name);
        name.setText(getArguments().getString(KEY_NAME));
        description = (EditText) layout.findViewById(R.id.description);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.song_details)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        songDetailsListener.OnSongDetailsEntered(name.getText().toString().trim(),
                                description.getText().toString().trim());
                    }
                })
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        songDetailsListener.OnCancel();
        super.onCancel(dialog);
    }
}
