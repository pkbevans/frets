package com.bondevans.frets.freteditor;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.app.AlertDialog;

import com.bondevans.frets.R;
import com.bondevans.frets.utils.Log;

public class GroupNotesAtFretDialog extends DialogFragment {

    private static final String TAG = GroupNotesAtFretDialog.class.getSimpleName();
    private int fret;
    private OkListener okListener;

    interface OkListener{
        void onOkClicked(int fret);
    }

    void setOkListener (OkListener okListener){
        Log.d(TAG, "setOkListener");
        this.okListener = okListener;
    }

    void setFret(int fret){
        Log.d(TAG, "setOkListener");
        this.fret = fret;
    }
    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");
        return new AlertDialog.Builder(getActivity())
                .setMessage(String.format(getString(R.string.group_frets)," "+fret))
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "HELLO - OK Pressed");
                        okListener.onOkClicked(fret);
                        dismiss();
                    }
                })
                .create();
    }
}
