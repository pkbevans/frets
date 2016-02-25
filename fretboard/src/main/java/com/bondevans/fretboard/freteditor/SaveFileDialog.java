package com.bondevans.fretboard.freteditor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.bondevans.fretboard.R;

public class SaveFileDialog extends DialogFragment {

    private SaveFileListener mSaveFileListener;

    public interface SaveFileListener {
        void onSave();

        void onCancel();
    }

    public void SetSaveFileListener(SaveFileListener saveFileListener) {
        mSaveFileListener = saveFileListener;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.save_file)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mSaveFileListener.onSave();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        mSaveFileListener.onCancel();
                    }
                })
                .create();
    }
}
