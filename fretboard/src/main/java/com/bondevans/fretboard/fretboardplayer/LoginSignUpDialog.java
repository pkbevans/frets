package com.bondevans.fretboard.fretboardplayer;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LoginSignUpDialog extends DialogFragment {

    public static final String TAG = "LoginSignUpDialog";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = View.inflate(getActivity(), R.layout.login_signup_dialog, null);
        getDialog().setTitle(R.string.login_register);
        return layout;
    }
}
