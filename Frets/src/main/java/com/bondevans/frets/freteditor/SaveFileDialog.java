package com.bondevans.frets.freteditor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.bondevans.frets.R;

public class SaveFileDialog extends DialogFragment {

    private SaveFileListener mSaveFileListener;

    interface SaveFileListener {
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
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        mSaveFileListener.onCancel();
                    }
                })
                .create();
    }
}
