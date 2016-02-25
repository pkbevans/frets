package com.bondevans.fretboard.fretlist;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.bondevans.fretboard.R;
import com.bondevans.fretboard.app.FretApplication;
import com.bondevans.fretboard.auth.LoginDialog;
import com.bondevans.fretboard.auth.LoginSignUpDialog;
import com.bondevans.fretboard.auth.SignUpDialog;
import com.bondevans.fretboard.filebrowser.FileBrowserActivity;
import com.bondevans.fretboard.firebase.FBWrite;
import com.bondevans.fretboard.firebase.dao.SongContents;
import com.bondevans.fretboard.firebase.dao.Songs;
import com.bondevans.fretboard.firebase.dao.Users;
import com.bondevans.fretboard.fretviewer.FretViewActivity;
import com.bondevans.fretboard.utils.FileLoaderTask;
import com.bondevans.fretboard.utils.FileWriterTask;
import com.bondevans.fretboard.utils.Log;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.File;

public class FretListActivity extends ListActivity {
    private static final String TAG = FretListActivity.class.getSimpleName();
    private static final String SETTINGS_KEY_PASSWORD = "pwd";
    private static final String SETTINGS_KEY_EMAIL = "email";
    private static final String TAG_LOGINSIGNUP = "LoginSignUpDialog";
    private static final String TAG_LOGIN = "Login";
    private static final String TAG_SIGNUP = "SignUp";
    private static final String SETTINGS_KEY_UID = "Uid";
    private static final String SETTINGS_KEY_USERNAME = "Username";
    private static final int ARBITRARY_LARGE_NUMBER = 100000;
    private Firebase mFirebaseRef;
    private Firebase mFirebaseAuthRef;
    private ValueEventListener mConnectedListener;
    private FretListAdapter mFretListAdapter;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fretlist_layout);
        authenticateUser();
        // Setup our Firebase
        mFirebaseRef = new Firebase(getString(R.string.firebase_url)).child("songs");
        // Setup the progress dialog that is displayed later
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.buffering_msg));
        progressDialog.setCancelable(false);
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
                return (true);
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchBrowser() {
        Intent intent = new Intent(this, FileBrowserActivity.class);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FileBrowserActivity");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as description changes
        final ListView listView = getListView();
        // Tell our list adapter that we only want 50 messages at a time
        mFretListAdapter = new FretListAdapter(mFirebaseRef, this, R.layout.fretlist_item);
        listView.setAdapter(mFretListAdapter);
        mFretListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mFretListAdapter.getCount() - 1);
            }
        });

        // Finally, a little indication of connection status
        progressDialog.show();
        mConnectedListener = mFirebaseRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                progressDialog.hide();
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Log.d(TAG, "Connected to Firebase");
                } else {
                    Log.d(TAG, "Disconnected from Firebase");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseRef.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
        mFretListAdapter.cleanup();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Show progress bar
        progressDialog.show();
        Songs song = (Songs) getListView().getItemAtPosition(position);
        Log.d(TAG, "onListItemClick: " + song.getId());
        // See if we've got this song in the cache
        final File cacheFile = new File(getExternalFilesDir(null), song.getId() + ".xml");
        if(cacheFile.exists()){
            if (cacheFile.length() > ARBITRARY_LARGE_NUMBER) {
                Log.d(TAG, "File size too big: " + cacheFile.length());
                showFretView(cacheFile);
            } else {
                // Get the file
                Log.d(TAG, "Got file in cache: " + cacheFile.getName());
                FileLoaderTask fileLoader = new FileLoaderTask(cacheFile);
                fileLoader.setFileLoadedListener(new FileLoaderTask.FileLoadedListener() {
                    @Override
                    public void OnFileLoaded(String contents) {
                        Log.d(TAG, "File loaded");
                        showFretView(cacheFile, contents);
                    }

                    @Override
                    public void OnError(String msg) {
                        progressDialog.hide();
                        Toast.makeText(FretListActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
                fileLoader.execute();
            }
        } else {
            Log.d(TAG, "NOT in cache: " + cacheFile.getName());
            // Get the SongContent from the server
            Firebase songRef = new Firebase(getString(R.string.firebase_url)).child(SongContents.childName).child(song.getId());
            songRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String songContents = dataSnapshot.child("contents").toString();
                    Log.d(TAG, "HELLO CHILD:" + songContents);
                    if (songContents.length() > ARBITRARY_LARGE_NUMBER) {
                        Log.d(TAG, "File size too big: " + cacheFile.length());
                        // Write out to cache and then show FretView
                        writeFileToCache(cacheFile, songContents, true);
                    } else {
                        // Write out to cache in background
                        writeFileToCache(cacheFile, songContents, false);
                        showFretView(cacheFile, songContents);
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    progressDialog.hide();
                    Log.d(TAG, "OOPS " + firebaseError.getMessage());
                    Toast.makeText(FretListActivity.this, firebaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        // Write out a click on this song
        FretApplication app = (FretApplication) getApplicationContext();
        FBWrite.usage(mFirebaseRef.getRoot(), app.getUID(), song.getId());
    }

    private void showFretView(File cacheFile) {
        progressDialog.hide();
        Intent intent = new Intent(this, FretViewActivity.class);
        intent.setData(Uri.fromFile(cacheFile));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretViewActivity");
        }
    }

    private void showFretView(File fretFile, String songContents) {
        progressDialog.hide();
        Intent intent = new Intent(this, FretViewActivity.class);
        intent.putExtra(FretViewActivity.INTENT_SONGCONTENTS, songContents);
        // Need the file as well as the contents so that the Freteditor can save updates back to the file.
        intent.setData(Uri.fromFile(fretFile));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NO ACTIVITY FOUND: FretViewActivity");
        }
    }

    private void writeFileToCache(final File cacheFile, String songContents, final boolean showFretView) {
        FileWriterTask fileWriterTask = new FileWriterTask(cacheFile, songContents);
        fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
            @Override
            public void OnFileWritten() {
                Log.d(TAG, "File written to cache: " + cacheFile.getName());
                if (showFretView) {
                    showFretView(cacheFile);
                }
            }

            @Override
            public void OnError(String msg) {
                progressDialog.hide();
                Toast.makeText(FretListActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        fileWriterTask.execute();
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
