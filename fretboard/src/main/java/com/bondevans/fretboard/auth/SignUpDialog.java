package com.bondevans.fretboard.auth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.utils.Log;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public class SignUpDialog extends DialogFragment {

    public static final String TAG = "SignUpDialog";
    private NewUserListener newUserListener;
    private Firebase mFirebase;
    /* A dialog that is presented until the Firebase authentication finished. */
    private ProgressDialog mAuthProgressDialog;

    public interface NewUserListener {
        void OnNewUser(AuthData authData, String email, String pwd);
    }

    public void setNewUserListener(NewUserListener newUserListener) {
        this.newUserListener = newUserListener;
    }

    public void setFirebase( Firebase firebase){
        this.mFirebase = firebase;
    }

    public static SignUpDialog newInstance() {
        SignUpDialog frag = new SignUpDialog();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle args) {
        final EditText email;
        final EditText pwd1;
        final EditText pwd2;
        Button signupButton;
        final TextView status;

        View layout = View.inflate(getActivity(), R.layout.signup_dialog, null);
        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(getActivity());
        mAuthProgressDialog.setTitle("Please wait");
        mAuthProgressDialog.setMessage(getString(R.string.authenticating));
        mAuthProgressDialog.setCancelable(false);

        status = (TextView) layout.findViewById(R.id.statusText);
        email = (EditText) layout.findViewById(R.id.email);
        pwd1 = (EditText) layout.findViewById(R.id.password1);
        pwd2 = (EditText) layout.findViewById(R.id.password2);

        signupButton = (Button) layout.findViewById(R.id.signUpButton);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "HELLO Signup button clicked");
                if (email.getText().toString().isEmpty() ||
                        pwd1.getText().toString().isEmpty() ||
                        pwd2.getText().toString().isEmpty()) {
                    status.setText(getString(R.string.create_user_txt));
                } else if (pwd1.getText().toString().compareTo(pwd2.getText().toString())!=0) {
                    status.setText(getString(R.string.password_mismatch));
                } else {
                    Log.d(TAG, "HELLO adding user...");
                    mAuthProgressDialog.show();
                    mFirebase.createUser(email.getText().toString().trim(), pwd1.getText().toString().trim(), new Firebase.ResultHandler() {
                        @Override
                        public void onSuccess() {
                            mFirebase.authWithPassword(email.getText().toString().trim(), pwd1.getText().toString().trim(), new Firebase.AuthResultHandler() {
                                @Override
                                public void onAuthenticated(AuthData authData) {
                                    mAuthProgressDialog.hide();
                                    Log.d(TAG, "HELLO Authentication OK");
                                    newUserListener.OnNewUser(authData, email.getText().toString().trim(), pwd1.getText().toString().trim());
                                    // Close dialog
                                    dismiss();
                                }

                                @Override
                                public void onAuthenticationError(FirebaseError firebaseError) {
                                    mAuthProgressDialog.hide();
                                    switch (firebaseError.getCode()) {
                                        case FirebaseError.INVALID_PASSWORD:
                                            Log.d(TAG, "HELLO INVALID PASSWORD");
                                            status.setText(R.string.invalid_pwd);
                                            break;
                                        case FirebaseError.DISCONNECTED:
                                        case FirebaseError.NETWORK_ERROR:
                                            status.setText(R.string.connection_error);
                                            break;
                                        case FirebaseError.EMAIL_TAKEN:
                                            status.setText(R.string.email_taken);
                                            break;
                                        case FirebaseError.INVALID_EMAIL:
                                            status.setText(R.string.invalid_email);
                                            break;
                                        default:
                                            status.setText(R.string.oops);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(FirebaseError firebaseError) {
                            Log.d(TAG, "HELLO Authentication FAILED: " + firebaseError.getCode() + " - " + firebaseError.getMessage());
                            mAuthProgressDialog.hide();
                            switch (firebaseError.getCode()) {
                                case FirebaseError.INVALID_PASSWORD:
                                    Log.d(TAG, "HELLO INVALID PASSWORD");
                                    status.setText(R.string.invalid_pwd);
                                    break;
                                case FirebaseError.DISCONNECTED:
                                case FirebaseError.NETWORK_ERROR:
                                    status.setText(R.string.connection_error);
                                    break;
                                case FirebaseError.EMAIL_TAKEN:
                                    status.setText(R.string.email_taken);
                                    break;
                                case FirebaseError.INVALID_EMAIL:
                                    status.setText(R.string.invalid_email);
                                    break;
                                default:
                                    status.setText(R.string.oops);
                            }
                        }
                    });
                }
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.sign_up)
                .setView(layout)
                .create();
    }
}
