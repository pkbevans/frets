package com.bondevans.fretboard.filebrowser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.bondevans.fretboard.R;

public class SongDetailsDialog extends DialogFragment {

    public static final String TAG = SongDetailsDialog.class.getSimpleName();
    private SongDetailsListener songDetailsListener;

    public interface SongDetailsListener {
        void OnLoginDetailsEntered(String name, String description);
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

        name = (EditText) layout.findViewById(R.id.name);
        description = (EditText) layout.findViewById(R.id.description);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.song_details)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        songDetailsListener.OnLoginDetailsEntered(name.getText().toString().trim(),
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
