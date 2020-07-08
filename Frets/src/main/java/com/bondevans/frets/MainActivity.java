package com.bondevans.frets;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.firebase.dao.UserProfile;
import com.bondevans.frets.fretlist.FretListActivity;
import com.bondevans.frets.utils.Log;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4523;
    private static final int RC_SIGN_IN = 1;
    private static final int REQUEST_FRETLIST = 4621;
    private static final int REQUEST_WELCOME = 4622;
    private FretApplication mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.mainactivity_layout);
        checkFileAccessPermission();
        mApp = (FretApplication) getApplicationContext();
        authenticateUser();
    }

    private void setUser(final FirebaseUser fbUser, UserProfile userProfile){
        // Get the Profile from the server if userProfile == null
        if(userProfile == null) {
            DatabaseReference profileRef = FirebaseDatabase.getInstance().getReference().child("users").child(fbUser.getUid()).child("userProfile");
            profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "HELLO CHILD:" + dataSnapshot.toString());
                    // load into a class, then set UI values
                    mApp.setUser(fbUser.getUid(), dataSnapshot.getValue(UserProfile.class));
                    launchFretListActivity();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "OOPS " + databaseError.getMessage());
                }
            });
        } else {
            mApp.setUser(fbUser.getUid(),userProfile);
            launchFretListActivity();
        }
    }
    private void authenticateUser(){
        Log.d(TAG, "HELLO authenticateUser");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // already signed in
            String msg = "HELLO User Signed in email:"+ auth.getCurrentUser().getEmail() + " id:"+auth.getCurrentUser().getUid();
            Log.d(TAG, msg);
            setUser(auth.getCurrentUser(), null);
        } else {
            Log.d(TAG, "HELLO user not signed in");
            // not signed in
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build());

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false)
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        }
    }
    private void launchFretListActivity() {
        Log.d(TAG, "HELLO Launching FretList for "+mApp.getUserName());
        try {
            startActivityForResult(new Intent(this, FretListActivity.class), REQUEST_FRETLIST);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "FretListActivity NOT FOUND");
        }
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: "+requestCode+":"+resultCode);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Log.d(TAG, "HELLO New User: SUCCESS: "+user.getEmail()+ ":"+user.getUid() +
                            ":"+user.getDisplayName()+":"+ user.getDisplayName());
                // // Add a new User Profile to Firebase
                checkExistingAndAddUserIfNew(user);
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Log.d(TAG, "HELLO onActivityResult: FAILED - Closing APP");
                finish();
            }
        } else if(requestCode == REQUEST_FRETLIST){
            Log.d(TAG, "Lets get outa here");
            finish();
        } else if (requestCode == REQUEST_WELCOME){
            Log.d(TAG, "Launching Fretlist");
            launchFretListActivity();
        }
    }

    private void checkExistingAndAddUserIfNew(final FirebaseUser user) {
        // Check whether this user already exists
        // Get the Profile from the server
        DatabaseReference profileRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("userProfile");
        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "HELLO CHILD:" + dataSnapshot.toString());
                if(!dataSnapshot.exists()){
                    Log.d(TAG, "HELLO New User");
                    UserProfile userProfile = new UserProfile(user.getDisplayName(), user.getEmail(),"", "", new Date().getTime());
                    FBWrite.addUserProfile(FirebaseDatabase.getInstance().getReference(), userProfile, user.getUid());
                    mApp.setUser(user.getUid(), userProfile);
                    launchWelcomeScreen();
                }else{
                    Log.d(TAG, "HELLO Existing User");
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    setUser(user, userProfile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "OOPS " + databaseError.getMessage());
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchWelcomeScreen() {
        //TODO
        launchFretListActivity();
    }
}