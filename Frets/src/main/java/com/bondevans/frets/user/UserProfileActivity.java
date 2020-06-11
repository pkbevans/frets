package com.bondevans.frets.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.bondevans.frets.R;
import com.bondevans.frets.utils.Log;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.bondevans.frets.user.UserProfileFragment.*;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private UserProfileFragment fragment;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_activity);
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        fragment = (UserProfileFragment) getSupportFragmentManager().findFragmentById(R.id.fragment2);
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        progressBar = findViewById(R.id.progress_bar); // Attaching the layout to the toolbar object

        if (savedInstanceState == null) {
            //  We should have the User ID  in the intent
            Intent intent = getIntent();
            String uid = intent.getStringExtra(INTENT_UID);
            Boolean editable = intent.getBooleanExtra(INTENT_EDITABLE, false);
            Log.d(TAG, "Got User ID: "+uid);
            // set UID in fragment
            fragment.loadUserProfile(uid, editable);
        }
        else{
            Log.d(TAG, "CONFIG CHANGE");
            getSupportActionBar().setTitle("User Profile");
        }
    }
}