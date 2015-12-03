package com.bondevans.fretboard.filebrowser;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.app.FretApplication;
import com.bondevans.fretboard.auth.LoginDialog;
import com.bondevans.fretboard.auth.LoginSignUpDialog;
import com.bondevans.fretboard.auth.SignUpDialog;
import com.bondevans.fretboard.firebase.FBWrite;
import com.bondevans.fretboard.firebase.dao.Usage;
import com.bondevans.fretboard.fretview.FretSong;
import com.bondevans.fretboard.fretview.FretSongLoader;
import com.bondevans.fretboard.midi.MidiImporter;
import com.bondevans.fretboard.player.FretViewActivity;
import com.bondevans.fretboard.utils.Log;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.io.File;

public class FileBrowserActivity extends Activity implements
        FileBrowserFragment.OnFileSelectedListener {
    private static final String TAG = "FileBrowserActivity";
    private static final int REFRESH_ID = Menu.FIRST + 16;
    private static final int UP_ID = Menu.FIRST + 17;
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4523;
    private static final String SETTINGS_KEY_PASSWORD = "pwd";
    private static final String SETTINGS_KEY_EMAIL = "email";
    private static final String TAG_LOGINSIGNUP = "LoginSignUpDialog";
    private static final String TAG_LOGIN = "Login";
    private static final String TAG_SIGNUP = "SignUp";
    private static final String TAG_SONGDETAILS = "SongDets";
    private static final String SETTINGS_KEY_UID = "Uid";

    private FileBrowserFragment fileBrowserFragment = null;
    private boolean mUpEnabled = false;
    private Menu mMenu = null;
    /* A reference to the Firebase */
    private Firebase mFirebaseRef;
    private String mUid;
    private String mEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HELLO onCreate");
        checkFileAccessPermission();
        firebaseStuff();
        setContentView(R.layout.file_browser_activity);// This is the xml with all the different frags
        ActionBar x = getActionBar();
        if (x == null) Log.d(TAG, "HELLO NO ACTION BAR");
        FragmentManager fm = getFragmentManager();
        fileBrowserFragment = (FileBrowserFragment) fm.findFragmentById(R.id.browser_fragment);
    }


    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onCreateOptionsMenu(com.actionbarsherlock.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;

        if (mUpEnabled) {
            menu.add(0, UP_ID, 1, getString(R.string.up))
                    .setIcon(R.drawable.up_button)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        menu.add(0, REFRESH_ID, 0, getString(R.string.refresh))
//                .setIcon(R.drawable.ai_refresh)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
                // Back to Songs
                finish();
                break;
            case REFRESH_ID:
                fileBrowserFragment.refresh();
                break;
            case UP_ID:
                upOneLevel(null);
                break;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "HELLO on SaveInstanceState");

        super.onSaveInstanceState(outState);
    }

//    @Override
    public void onFileSelected(File file) {
//        if(file.getName().endsWith("xml")){
            // Record Usage
//            FBWrite.usage(mFirebaseRef, mUid, Usage.FEATURE_BROWSETO_FILE);
            // Open the file with the FretViewActivity
//            Intent intent = new Intent(this, FretViewActivity.class);
//            intent.setData(Uri.fromFile(file));
//            try {
//                startActivity(intent);
//            } catch (ActivityNotFoundException e) {
//                Log.e(TAG, "NO ACTIVITY FOUND: FretViewActivity");
//            }
//        }
//        else
        if(file.getName().endsWith("mid")) {
            // Import the midi file into an instance of FretSong
            // write out to file in cache (/sdcard/android.com.bondevans.fretplayer....)
            MidiImporter midiImporter = new MidiImporter(file,
                    new File(getExternalFilesDir(null), file.getName() + ".xml"));
            midiImporter.setFileImportedListener(new MidiImporter.FileImportedListener() {
                @Override
                public void OnImportedLoaded(final File file) {
                    Toast.makeText(FileBrowserActivity.this, "Imported to file:"+file.getName(), Toast.LENGTH_SHORT).show();
                    // Get the Song name and description
                    SongDetailsDialog songDetailsDialog = new SongDetailsDialog();
                    songDetailsDialog.setSongDetailsListener(new SongDetailsDialog.SongDetailsListener() {
                        @Override
                        public void OnLoginDetailsEntered(String name, String description) {
                            // Write to server
                            writeSongToServer(file, name, description);
                        }

                        @Override
                        public void OnCancel() {
                            Toast.makeText(FileBrowserActivity.this, "Imported cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });
                    songDetailsDialog.show(getFragmentManager(), TAG_SONGDETAILS);
                }

                @Override
                public void OnError(String msg) {
                    Toast.makeText(FileBrowserActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
            midiImporter.execute();
        }
        else{
            Toast.makeText(FileBrowserActivity.this, R.string.invalid_file_type, Toast.LENGTH_SHORT).show();
        }
    }

    private void writeSongToServer(File file, final String name, final String description) {
        FretSongLoader fretSongLoader = new FretSongLoader(file);
        fretSongLoader.setSongLoadedListener(new FretSongLoader.SongLoadedListener() {
            @Override
            public void OnSongLoaded(FretSong fretSong) {
                fretSong.setName(name);
                FBWrite.addSong(mFirebaseRef, fretSong, description);
            }

            @Override
            public void OnError(String msg) {
                Toast.makeText(FileBrowserActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        fretSongLoader.execute();
    }

    @Override
    public void upOneLevel(View v) {
        fileBrowserFragment.upOneLevel();
    }

    @Override
    public void enableUp(boolean enable) {
        if (mMenu != null) {
            // If not already enabled, enable the UP button
            if (enable && !mUpEnabled) {
                mMenu.add(0, UP_ID, 1, getString(R.string.up))
                        .setIcon(R.drawable.up_button)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            //Remove the button if we are disabling it
            else if (!enable) {
                mMenu.removeItem(UP_ID);
            }
        }
        mUpEnabled = enable;
    }

    private void checkFileAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.util.Log.d(TAG, "checkFileAccessPermission 1");
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d(TAG, "checkFileAccessPermission 2");
                // Need to request permission from the user
                String[] perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(perms, REQUEST_CODE_READ_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // TODO need to handle user not allowing access.
        Log.d(TAG, "onRequestPermissionsResult");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void firebaseStuff() {
        Log.d(TAG, "HELLO firebaseStuff");
//        Firebase.setAndroidContext(this);
        /* Create the Firebase ref that is used for all authentication with Firebase */
        mFirebaseRef = new Firebase(getResources().getString(R.string.firebase_url));
        /* Check if the user is authenticated with Firebase already. If this is the case we can set the authenticated
         * user and hide hide any login buttons */

        // See if we have stored the pwd for this user yet
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        mUid = settings.getString(SETTINGS_KEY_UID, "");
        mEmail = settings.getString(SETTINGS_KEY_EMAIL, "");
        String pwd = settings.getString(SETTINGS_KEY_PASSWORD, "");
        if (pwd.isEmpty()) {
            Log.d(TAG, "HELLO No stored pwd - launching login dialog");
            // First time, so show login/registration dialog
            showLoginSignUpDialog();
        } else {
            Log.d(TAG, "Authenticating with existing details");
            // Use the email/pwd to login
            mFirebaseRef.authWithPassword(mEmail, pwd, new AuthResultHandler("password", mEmail, pwd));
        }

        editor.apply(); // Use apply rather than commit to do it in background
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
        dialog.setFirebase(mFirebaseRef);
        dialog.setNewUserListener(new SignUpDialog.NewUserListener() {
            @Override
            public void OnNewUser(AuthData authData, String email, String pwd) {
                // Save New user details
                FBWrite.newUser(FileBrowserActivity.this, mFirebaseRef, authData.getUid(), email);
                Log.d(TAG, "Got new User details");
                setAuthenticatedUser(authData, email, pwd);
            }
        });
        dialog.show(getFragmentManager(), TAG_SIGNUP);
    }

    void showLoginDialog(String email, String pwd) {
        LoginDialog dialog = LoginDialog.newInstance(email, pwd);
        dialog.setFirebase(mFirebaseRef);
        dialog.setLoginListener(new LoginDialog.LoginListener() {
            @Override
            public void OnLoginDetailsEntered(AuthData authData, String email, String pwd) {
                Log.d(TAG, "HELLO OnLoginDetailsEntered");
                // Store the details
                // First login on this device (or the user has cleared the description)
                FBWrite.newUser(FileBrowserActivity.this, mFirebaseRef, authData.getUid(), email);
                setAuthenticatedUser(authData, email, pwd);
            }

            @Override
            public void OnReset(String email) {
                mFirebaseRef.resetPassword(email, new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "resetUser - SUCCESS");
                        Toast.makeText(FileBrowserActivity.this, R.string.reset_msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FirebaseError firebaseError) {
                        // TODO - Do Something
                        Toast.makeText(FileBrowserActivity.this, firebaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
    private void setAuthenticatedUser(AuthData authData, String email, String pwd) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        if (authData != null) {
            Log.d(TAG, "HELLO - authdata NOT null");
            /* show a provider specific status text */
            Toast.makeText(this, "Authenticated", Toast.LENGTH_SHORT).show();
            if (authData.getProvider().equals("anonymous")
                    || authData.getProvider().equals("password")) {
                FretApplication app = (FretApplication) getApplicationContext();
                app.setAuthID(authData.getUid());
                editor.putString(SETTINGS_KEY_UID, authData.getUid());
                editor.putString(SETTINGS_KEY_EMAIL, email);
                editor.putString(SETTINGS_KEY_PASSWORD, pwd);
                mEmail = email;
                mUid = authData.getUid();
            } else {
                Log.e(TAG, "Invalid provider: " + authData.getProvider());
            }
        } else {
            Log.d(TAG, "HELLO - authdata null - TODO");
            // Delete the pwd to force login dialog again
            editor.remove(SETTINGS_KEY_PASSWORD);
            // Not authenticated - show the Login/registration screen next time
            Toast.makeText(this, "Oops! Not Authenticated", Toast.LENGTH_SHORT).show();
        }
        editor.apply(); // Use apply rather than commit to do it in background
    }

    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {
        private final String provider;
        private String email;
        private String pwd;

        public AuthResultHandler(String provider, String email, String pwd) {
            this.provider = provider;
            this.email = email;
            this.pwd = pwd;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            Log.d(TAG, provider + " auth successful");
            setAuthenticatedUser(authData, email, pwd);
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.d(TAG, provider + " auth ERROR: [" + firebaseError.getCode() + "] - " + firebaseError.toString());
            switch (firebaseError.getCode()) {
                case FirebaseError.INVALID_EMAIL:
                case FirebaseError.INVALID_PASSWORD:
                    Log.d(TAG, "HELLO showing login dialog");
                    showLoginDialog(email, "");
                    break;
                case FirebaseError.USER_DOES_NOT_EXIST:
                    Log.d(TAG, "HELLO showing login dialog");
                    showSignUpDialog();
                    break;
                case FirebaseError.NETWORK_ERROR:
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