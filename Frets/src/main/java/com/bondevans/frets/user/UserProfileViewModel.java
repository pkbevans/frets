package com.bondevans.frets.user;

import android.graphics.Bitmap;

import com.bondevans.frets.firebase.dao.UserProfile;

import androidx.lifecycle.ViewModel;

public class UserProfileViewModel extends ViewModel {
    private Bitmap profileBitmap;
    private UserProfile userProfile;

    public Bitmap getProfileBitmap() {
        return profileBitmap;
    }

    public void setProfileBitmap(Bitmap profileBitmap) {
        this.profileBitmap = profileBitmap;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }
}