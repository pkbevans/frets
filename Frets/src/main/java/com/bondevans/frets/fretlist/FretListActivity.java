package com.bondevans.frets.fretlist;

import android.Manifest;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.auth.LoginDialog;
import com.bondevans.frets.auth.LoginSignUpDialog;
import com.bondevans.frets.auth.SignUpDialog;
import com.bondevans.frets.filebrowser.FileBrowserActivity;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.firebase.dao.Users;
import com.bondevans.frets.utils.Log;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import static android.support.v4.content.PermissionChecker.PERMISSION_DENIED;

public class FretListActivity extends AppCompatActivity {
    private static final String TAG = FretListActivity.class.getSimpleName();
    private static final String SETTINGS_KEY_PASSWORD = "pwd";
    private static final String SETTINGS_KEY_EMAIL = "email";
    private static final String TAG_LOGINSIGNUP = "LoginSignUpDialog";
    private static final String TAG_LOGIN = "Login";
    private static final String TAG_SIGNUP = "SignUp";
    private static final String SETTINGS_KEY_UID = "Uid";
    private static final String SETTINGS_KEY_USERNAME = "Username";
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4523;
    private Firebase mFirebaseAuthRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fretlist_activity);
        checkFileAccessPermission();
        authenticateUser();

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fretlist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        int id = item.getItemId();
        switch (id) {
            case R.id.action_import:
                launchBrowser();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchMidiTest() {
        Intent intent = new Intent(this, MidiTest.class);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: "+MidiTest.class.getSimpleName());
        }
    }

    private void launchBrowser() {
        Intent intent = new Intent(this, FileBrowserActivity.class);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: "+FileBrowserActivity.class.getSimpleName());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void authenticateUser() {
        Log.d(TAG, "HELLO authenticateUser");
        /* Create the Firebase ref that is used for authentication with Firebase */
        mFirebaseAuthRef = new Firebase(getString(R.string.firebase_url));
        /* Check if the user is authenticated with Firebase already. If this is the case we can set the authenticated
         * user and hide hide any login buttons */

        // See if we have stored the pwd for this user yet
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Users user = new Users(settings.getString(SETTINGS_KEY_USERNAME, ""), settings.getString(SETTINGS_KEY_EMAIL, ""));
        String pwd = settings.getString(SETTINGS_KEY_PASSWORD, "");
        if (pwd.isEmpty()) {
            Log.d(TAG, "HELLO No stored pwd - launching login dialog");
            // First time, so show login/registration dialog
            showLoginSignUpDialog();
        } else {
            Log.d(TAG, "Authenticating with existing details");
            // Use the email/pwd to login
            mFirebaseAuthRef.authWithPassword(user.getEmail(), pwd, new AuthResultHandler("password", user, pwd));
        }
    }

    void showLoginSignUpDialog() {
        LoginSignUpDialog dialog = new LoginSignUpDialog();
        dialog.show(getFragmentManager(), TAG_LOGINSIGNUP);
    }

    public void showSignUp(View v) {
        Log.d(TAG, "HELLO - showSignUp");
        showSignUpDialog();
        // Kill the LoginSignUp dialog
        killDialog(TAG_LOGINSIGNUP);
    }

    public void showLogin(View v) {
        Log.d(TAG, "HELLO - showLogin");
        showLoginDialog("", "");
        // Kill the LoginSignUp dialog
        killDialog(TAG_LOGINSIGNUP);
    }

    void killDialog(String tag) {
        DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(tag);
        if (dialog != null) dialog.dismiss();
    }

    void showSignUpDialog() {
        SignUpDialog dialog = SignUpDialog.newInstance();
        dialog.setFirebase(mFirebaseAuthRef);
        dialog.setNewUserListener(new SignUpDialog.NewUserListener() {
            @Override
            public void OnNewUser(AuthData authData, Users user, String pwd) {
                // Save New user details
                FBWrite.newUser(FretListActivity.this, mFirebaseAuthRef, authData.getUid(), user);
                Log.d(TAG, "Got new User details");
                setAuthenticatedUser(authData, user, pwd);
            }
        });
        dialog.show(getFragmentManager(), TAG_SIGNUP);
    }

    void showLoginDialog(String email, String pwd) {
        LoginDialog dialog = LoginDialog.newInstance(email, pwd);
        dialog.setFirebase(mFirebaseAuthRef);
        dialog.setLoginListener(new LoginDialog.LoginListener() {
            @Override
            public void OnLoginDetailsEntered(AuthData authData, Users user, String pwd) {
                Log.d(TAG, "HELLO OnLoginDetailsEntered");
                // Store the details
                // First login on this device (or the user has cleared the cache)
                FBWrite.newUser(FretListActivity.this, mFirebaseAuthRef, authData.getUid(), user);
                setAuthenticatedUser(authData, user, pwd);
            }

            @Override
            public void OnReset(String email) {
                mFirebaseAuthRef.resetPassword(email, new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "resetUser - SUCCESS");
                        Toast.makeText(FretListActivity.this, R.string.reset_msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FirebaseError firebaseError) {
                        // TODO - Do Something
                        Toast.makeText(FretListActivity.this, firebaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "resetUser - ERROR" + firebaseError.getMessage());
                    }
                });
            }
        });
        dialog.show(getFragmentManager(), TAG_LOGIN);
    }

    /**
     * Once a user is logged in, take the mAuthData provided from Firebase and "use" it.
     */
    private void setAuthenticatedUser(AuthData authData, Users user, String pwd) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        if (authData != null) {
            Log.d(TAG, "HELLO - authdata NOT null");
            /* show a provider specific status text */
            Toast.makeText(this, getString(R.string.hello_user) + user.getUsername(), Toast.LENGTH_SHORT).show();
            if (authData.getProvider().equals("anonymous")
                    || authData.getProvider().equals("password")) {
                FretApplication app = (FretApplication) getApplicationContext();
                app.setUID(authData.getUid());
                editor.putString(SETTINGS_KEY_UID, authData.getUid());
                editor.putString(SETTINGS_KEY_USERNAME, user.getUsername());
                editor.putString(SETTINGS_KEY_EMAIL, user.getEmail());
                editor.putString(SETTINGS_KEY_PASSWORD, pwd);
            } else {
                Log.e(TAG, "Invalid provider: " + authData.getProvider());
            }
        } else {
            Log.d(TAG, "HELLO - authdata null - TODO");
            // Delete the pwd to force login dialog again
            editor.remove(SETTINGS_KEY_PASSWORD);
            // Not authenticated - show the Login/registration screen next time
        }
        editor.apply(); // Use apply rather than commit to do it in background
    }

    private void checkFileAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "checkFileAccessPermission 1");
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "checkFileAccessPermission 2");
                // Need to request permission from the user
                String[] perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(perms, REQUEST_CODE_READ_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if( requestCode == REQUEST_CODE_READ_STORAGE_PERMISSION && grantResults[0] == PERMISSION_DENIED){
            // Handle user not allowing access.
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "onRequestPermissionsResult");
    }

    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {
        private final String provider;
        private Users user;
        private String pwd;

        public AuthResultHandler(String provider, Users user, String pwd) {
            this.provider = provider;
            this.user = user;
            this.pwd = pwd;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            Log.d(TAG, provider + " auth successful");
            setAuthenticatedUser(authData, user, pwd);
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.d(TAG, provider + " auth ERROR: [" + firebaseError.getCode() + "] - " + firebaseError.toString());
            switch (firebaseError.getCode()) {
                case FirebaseError.INVALID_EMAIL:
                case FirebaseError.INVALID_PASSWORD:
                    Log.d(TAG, "HELLO showing login dialog");
                    showLoginDialog(user.getEmail(), "");
                    break;
                case FirebaseError.USER_DOES_NOT_EXIST:
                    Log.d(TAG, "HELLO showing login dialog");
                    showSignUpDialog();
                    break;
                case FirebaseError.NETWORK_ERROR:
                    Toast.makeText(FretListActivity.this, R.string.no_network, Toast.LENGTH_SHORT).show();
                    break;
                case FirebaseError.DISCONNECTED:
                case FirebaseError.AUTHENTICATION_PROVIDER_DISABLED:
                case FirebaseError.INVALID_PROVIDER:
                case FirebaseError.OPERATION_FAILED:
                case FirebaseError.UNKNOWN_ERROR:
                default:
                    // Ignore
                    Log.d(TAG, "HELLO Ignoring error");

            }
        }
    }
}
