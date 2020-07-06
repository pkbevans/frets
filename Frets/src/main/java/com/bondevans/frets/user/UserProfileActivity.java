package com.bondevans.frets.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;

import com.bondevans.frets.R;
import com.bondevans.frets.utils.Log;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import static com.bondevans.frets.user.UserProfileFragment.INTENT_EDITABLE;
import static com.bondevans.frets.user.UserProfileFragment.INTENT_UID;

public class UserProfileActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private UserProfileFragment mFragment;
    private ProgressBar progressBar;
    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_activity);
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

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
            mFragment = (UserProfileFragment) UserProfileFragment.newInstance(uid, editable);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, mFragment)
                    .commitNow();
            FloatingActionButton fab = findViewById(R.id.fab);
            if(editable) {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        captureProfilePicture();
                    }
                });
            } else{
                fab.setVisibility(View.GONE);
            }
        }
        else{
            Log.d(TAG, "CONFIG CHANGE");
            getSupportActionBar().setTitle("User Profile");
        }
    }
    Uri mPhotoURI;
    private void captureProfilePicture() {
        File photoFile;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
                // Save a file: path for use with ACTION_VIEW intents
                mCurrentPhotoPath = photoFile.getAbsolutePath();
                Log.d(TAG, "HELLO photoFile: "+photoFile.getPath());
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d(TAG, "HELLO - ERROR");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                mPhotoURI = FileProvider.getUriForFile(this,
                        "com.bondevans.frets.fileprovider",
                        photoFile);
                // Tell the intent where to put the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Intent will be null, but file picture will be where we told it to be
            Log.d(TAG, "HELLO got picture.: "+mCurrentPhotoPath);
            mFragment.setNewProfilePic(new File(mCurrentPhotoPath));
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,      /* prefix */
                ".jpg",      /* suffix */
                storageDir          /* directory */
        );
        return imageFile;
    }
}