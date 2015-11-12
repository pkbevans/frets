package com.bondevans.fretboard.fretboardplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;

public class LoginSignUpDialog extends DialogFragment {

    public static final String TAG = "LoginSignUpDialog";

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle args) {

//        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = View.inflate(getActivity(), R.layout.login_signup_dialog, null);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.login_register)
                .setView(layout)
                .setCancelable(false)
                .create();
    }
}
