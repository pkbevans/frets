package com.bondevans.frets.user;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import static com.bondevans.frets.user.UserProfileFragment.INTENT_EDITABLE;
import static com.bondevans.frets.user.UserProfileFragment.INTENT_UID;

public class UserProfileActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_SELECT = 2;
    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private static final int MAX_IMAGE_DIMENSION = 1024;
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
                        Snackbar.make(view, "Select a Profile pic", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        selectProfilePicture();
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

    private void selectProfilePicture(){
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_SELECT);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Intent will be null, but file picture will be where we told it to be
            Log.d(TAG, "HELLO got picture.: "+mCurrentPhotoPath);
            mFragment.setNewProfilePic(new File(mCurrentPhotoPath));
        }else if(requestCode == REQUEST_IMAGE_SELECT ) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    Bitmap  bitmap = null;
                    try {
                        bitmap = scaleImage(this, uri);
//                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        File photoFile = createImageFile();
                        FileOutputStream out = new FileOutputStream(photoFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        mFragment.setNewProfilePic(photoFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.d(TAG, "User Cancelled");
            }
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
    private static Bitmap scaleImage(Context context, Uri photoUri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);
        is.close();

        int rotatedWidth, rotatedHeight;
        int orientation = getOrientation(context, photoUri);

        if (orientation == 90 || orientation == 270) {
            rotatedWidth = dbo.outHeight;
            rotatedHeight = dbo.outWidth;
        } else {
            rotatedWidth = dbo.outWidth;
            rotatedHeight = dbo.outHeight;
        }

        Bitmap srcBitmap;
        is = context.getContentResolver().openInputStream(photoUri);
        if (rotatedWidth > MAX_IMAGE_DIMENSION || rotatedHeight > MAX_IMAGE_DIMENSION) {
            float widthRatio = ((float) rotatedWidth) / ((float) MAX_IMAGE_DIMENSION);
            float heightRatio = ((float) rotatedHeight) / ((float) MAX_IMAGE_DIMENSION);
            float maxRatio = Math.max(widthRatio, heightRatio);

            // Create the bitmap from file
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) maxRatio;
            srcBitmap = BitmapFactory.decodeStream(is, null, options);
        } else {
            srcBitmap = BitmapFactory.decodeStream(is);
        }
        is.close();

        /*
         * if the orientation is not 0 (or -1, which means we don't know), we
         * have to do a rotation.
         */
        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                    srcBitmap.getHeight(), matrix, true);
        }

        String type = context.getContentResolver().getType(photoUri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (type.equals("image/png")) {
            srcBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        } else if (type.equals("image/jpg") || type.equals("image/jpeg")) {
            srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        }
        byte[] bMapArray = baos.toByteArray();
        baos.close();
        return BitmapFactory.decodeByteArray(bMapArray, 0, bMapArray.length);
    }

    public static int getOrientation(Context context, Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }
}