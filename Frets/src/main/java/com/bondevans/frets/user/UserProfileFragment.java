package com.bondevans.frets.user;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bondevans.frets.ImageUtils;
import com.bondevans.frets.R;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.firebase.dao.UserProfile;
import com.bondevans.frets.utils.Log;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class UserProfileFragment extends Fragment {
    private static final String TAG = UserProfileFragment.class.getSimpleName();
    public static final String INTENT_UID = "INTENT_UID";
    public static final String INTENT_EDITABLE = "INTENT_EDITABLE";
    private UserProfile mUserProfile;
    private ImageView mProfilePic;
    private EditText mUsername;
    private TextView mEmail;
    private EditText mBio;
    private EditText mWebsite;
    private TextView mDateJoined;
    DateFormat mSimpleDF;
    private Button mSaveButton;
    private String mUid;
    private StorageReference mStorageRef;
    private boolean mPhotoUpdated = false;
    private File mPhotoFile;
    private Bitmap mThumbNail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSimpleDF = new SimpleDateFormat("dd MMM yyyy");
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.user_profile_layout, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSaveButton = view.findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Update the profile, if changed
                if(mUserProfile.isUpdated()) {
                    updateUserProfile();
                }
                // Only upload pic if they have added a new one.
                if(mPhotoUpdated) {
                    uploadProfilePic(mPhotoFile, mUid);
                    uploadThumbnail(mUid);
                    // Copy thumbnail to cache
                    ImageUtils.writeThumbnailToCache(mUid,mThumbNail);
                }
                getActivity().finish();
            }
        });
        mProfilePic = view.findViewById(R.id.profile_pic);
        mUsername = view.findViewById(R.id.username);
        mEmail = view.findViewById(R.id.user_email);
        mBio = view.findViewById(R.id.bio);
        mWebsite = view.findViewById(R.id.website);
        mDateJoined = view.findViewById(R.id.date_joined);
    }

    private void updateUserProfile() {
        Log.d(TAG, "HELLO - saving User Profile to Firebase");

        mUserProfile.setUsername(mUsername.getText().toString());
        mUserProfile.setBio(mBio.getText().toString());
        mUserProfile.setWebsite(mWebsite.getText().toString());

        FBWrite.updateUser(FirebaseDatabase.getInstance().getReference(), mUserProfile,mUid);
    }

    void loadUserProfile(String uId, final Boolean editable){
        mUid = uId;
        // Get the Profile from the server
        DatabaseReference profileRef = FirebaseDatabase.getInstance().getReference().child("users").child(uId).child("userProfile");
        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "HELLO CHILD:" + dataSnapshot.toString());
                // load into a class, then set UI values
                mUserProfile = dataSnapshot.getValue(UserProfile.class);
                Log.d(TAG, "HELLO bio:" + mUserProfile.getBio());
                setUi(mUserProfile, editable);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "OOPS " + databaseError.getMessage());
                Toast.makeText(UserProfileFragment.this.getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        // Get the profile pic
        downloadProfilePic(uId);
    }

    private void setUi(UserProfile userProfile, Boolean editable) {
        String text = userProfile.getUsername().isEmpty()?"User name":userProfile.getUsername();
        mUsername.setText(text);
        mEmail.setText(userProfile.getEmail());
        text = userProfile.getBio().isEmpty()?"Bio":userProfile.getBio();
        mBio.setText(userProfile.getBio());
        text = userProfile.getWebsite().isEmpty()?"Website":userProfile.getWebsite();
        mWebsite.setText(userProfile.getWebsite());
        mDateJoined.setText(mSimpleDF.format(new Date(userProfile.getDateJoined())));
        if(!editable){
            mUsername.setEnabled(false);
            mBio.setEnabled(false);
            mWebsite.setEnabled(false);
            mUsername.setEnabled(false);
            mSaveButton.setVisibility(View.INVISIBLE);
        }
    }
    private void uploadProfilePic(File file, String uid){
        Log.d(TAG, "HELLO uploading file: "+file.getPath());
        StorageReference profilePicRef = mStorageRef.child("profilePictures").child(uid);
        Uri uri = Uri.fromFile(file);
        profilePicRef.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "HELLO uploaded file: "+taskSnapshot.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                    Log.d(TAG, "HELLO OOPS!!!: "+exception.getMessage());
                        // ...
                    }
                });
    }

    private void uploadThumbnail(String uid){
        StorageReference profilePicRef = mStorageRef.child("profileThumbnail").child(uid);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mThumbNail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = profilePicRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.d(TAG, "HELLO uploaded thumbnail FAILED: "+exception.getMessage());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "HELLO uploaded thumbnail");
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }

    private void downloadProfilePic(final String uid){
        StorageReference profilePicRef = mStorageRef.child("profilePictures").child(uid);
        try {
            mPhotoFile = File.createTempFile("images", "jpg");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "HELLO ERROR: "+e.getLocalizedMessage());
            return;
        }

        profilePicRef.getFile(mPhotoFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                Log.d(TAG, "Profile Pic Downloaded: "+mPhotoFile.getPath());
                setExistingProfilePic();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }
    void setNewProfilePic(File photoFile) {
        mPhotoFile = photoFile;
        Bitmap imageBitmap = ImageUtils.checkOrientation(mPhotoFile);
        Log.d(TAG, "HELLO - SetProfilePic: "+photoFile.getPath());
        mProfilePic.setImageBitmap(imageBitmap);
        mThumbNail = ThumbnailUtils.extractThumbnail(imageBitmap, 100, 100);
        mPhotoUpdated=true;
    }

    private void setExistingProfilePic(){
        // PIc will be in mPhotoFile
        Bitmap imageBitmap = ImageUtils.checkOrientation(mPhotoFile);
        mProfilePic.setImageBitmap(imageBitmap);
    }
}
