package com.bondevans.frets.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bondevans.frets.R;
import com.bondevans.frets.firebase.FBWrite;
import com.bondevans.frets.firebase.dao.UserProfile;
import com.bondevans.frets.utils.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private EditText mUsername;
    private TextView mEmail;
    private EditText mBio;
    private EditText mWebsite;
    private TextView mDateJoined;
    DateFormat mSimpleDF;
    private Button mSaveButton;
    private String mUid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSimpleDF = new SimpleDateFormat("dd MMM yyyy");
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
                // Update the profile
                updateUserProfile();
            }
        });
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
}
