package com.bondevans.fretboard.fretboardplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public class LoginDialog extends DialogFragment {

    public static final String TAG = "LoginDialog";
    private static final java.lang.String KEY_EMAIL = "e";
    private static final java.lang.String KEY_PWD = "p";
    private LoginListener loginListener;
    private Firebase mFirebase;
    /* A dialog that is presented until the Firebase authentication finished. */
    private ProgressDialog mAuthProgressDialog;

    public interface LoginListener {
        void OnLoginDetailsEntered(AuthData authData, String email, String pwd);

        void OnReset(String email);
    }

    public void setLoginListener(LoginListener loginListener) {
        this.loginListener = loginListener;
    }

    public void setFirebase( Firebase firebase){
        this.mFirebase = firebase;
    }
    public static LoginDialog newInstance(String email, String pwd) {
        LoginDialog frag = new LoginDialog();
        Bundle args = new Bundle();
        args.putString(KEY_EMAIL, email);
        args.putString(KEY_PWD, pwd);
        frag.setArguments(args);
        return frag;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle args) {
        final EditText email;
        final EditText pwd;
        Button reset;
        Button login;
        final TextView status;

//        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = View.inflate(getActivity(), R.layout.login_dialog, null);
        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(getActivity());
        mAuthProgressDialog.setTitle("Please wait");
        mAuthProgressDialog.setMessage(getString(R.string.authenticating));
        mAuthProgressDialog.setCancelable(false);

        status = (TextView) layout.findViewById(R.id.statusText);
        email = (EditText) layout.findViewById(R.id.email);
        pwd = (EditText) layout.findViewById(R.id.password1);
        email.setText(getArguments().getString(KEY_EMAIL));
        pwd.setText(getArguments().getString(KEY_PWD));

        login = (Button) layout.findViewById(R.id.loginButton);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "HELLO Login button clicked");
                //                    loginListener.OnLoginDetailsEntered(email.getText().toString().trim(), pwd.getText().toString().trim());
                if (email.getText().toString().isEmpty() ||
                        pwd.getText().toString().isEmpty()) {
                    status.setText(getString(R.string.login_txt));
                }
                else {
                    Log.d(TAG, "HELLO authenticating...");
                    mAuthProgressDialog.show();
                    mFirebase.authWithPassword(email.getText().toString().trim(), pwd.getText().toString().trim(), new Firebase.AuthResultHandler() {
                        @Override
                        public void onAuthenticated(AuthData authData) {
                            mAuthProgressDialog.hide();
                            Log.d(TAG, "HELLO Authentication OK");
                            loginListener.OnLoginDetailsEntered(authData, email.getText().toString().trim(), pwd.getText().toString().trim());
                            dismiss();
                        }

                        @Override
                        public void onAuthenticationError(FirebaseError firebaseError) {
                            Log.d(TAG, "HELLO Authentication FAILED: "+ firebaseError.getCode()+ " - " + firebaseError.getMessage());
                            mAuthProgressDialog.hide();
                            switch(firebaseError.getCode()){
                                case FirebaseError.INVALID_PASSWORD:
                                    Log.d(TAG, "HELLO INVALID PASSWORD");
                                    status.setText(R.string.invalid_pwd);
                                    break;
                                case FirebaseError.DISCONNECTED:
                                case FirebaseError.NETWORK_ERROR:
                                    status.setText(R.string.connection_error);
                                    break;
                                case FirebaseError.MAX_RETRIES:
                                    status.setText(R.string.max_retries_reached);
                                    break;
                                default:
                                    status.setText(R.string.oops);
                            }
                        }
                    });
                }
            }
        });
        reset = (Button) layout.findViewById(R.id.resetPwdButton);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send a reset password
                if (email.getText().toString().isEmpty()) {
                    status.setText(getString(R.string.enter_email_txt));
                } else {
                    loginListener.OnReset(email.getText().toString().trim());
                    // Close dialog
                    dismiss();
                }
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.login)
                .setView(layout)
                .setCancelable(false)
                .create();
    }
}
