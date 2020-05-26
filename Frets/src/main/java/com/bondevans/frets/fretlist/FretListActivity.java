package com.bondevans.frets.fretlist;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.filebrowser.FileBrowserActivity;
import com.bondevans.frets.utils.Log;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;

public class FretListActivity extends AppCompatActivity {
    private static final String TAG = FretListActivity.class.getSimpleName();
    private static final int REQUEST_CODE_READ_STORAGE_PERMISSION = 4523;
    private static final int RC_SIGN_IN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fretlist_activity);
        checkFileAccessPermission();
        authenticateUser();
        FretListPagerAdapter fretListPagerAdapter = new FretListPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(fretListPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        getSupportActionBar().setTitle("");             // Empty string - since we are using the logo image
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

    private void authenticateUser(){
        Log.d(TAG, "HELLO 1");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // already signed in
            String msg = "HELLO email:"+ auth.getCurrentUser().getEmail();
            Log.d(TAG, msg);
            FretApplication app = (FretApplication) getApplicationContext();
            app.setUID(auth.getCurrentUser().getUid());
        } else {
            Log.d(TAG, "HELLO 3");
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
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Log.d(TAG, "onActivityResult: SUCCESS");
                //
                FretApplication app = (FretApplication) getApplicationContext();
                app.setUID(user.getUid());
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // TODO - what to do if user fails to login?
                Log.d(TAG, "onActivityResult: FAILED - TODO");
            }
        }
    }
}
