package com.bondevans.frets.fretlist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bondevans.frets.ImageUtils;
import com.bondevans.frets.R;
import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.firebase.dao.Fret;
import com.bondevans.frets.firebase.dao.UserProfile;
import com.bondevans.frets.utils.FileLoader;
import com.bondevans.frets.utils.FileWriterTask;
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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class FretHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = FretHolder.class.getSimpleName();
    //Define all of your views here
    private final TextView mName;
    private final TextView mDescription;
    private final TextView mUser;
    private final TextView mInstrument;
    private final TextView mDateCreated;
    private ImageView mThumbnail;
    private final FretRecyclerViewClickListener mListener;
    final DateFormat simpleDF = new SimpleDateFormat("dd MMM yyyy");
    final File cacheDir = new File(FretApplication.getAppContext().getExternalCacheDir(),"" );

    //Define a constructor taking a View as its parameter
    public FretHolder(@NonNull View itemView, FretRecyclerViewClickListener listener) {
        super(itemView);
        //Remembered we defined an id attribute to our TextView in fretlist_item.xml
        mName = itemView.findViewById(R.id.name);
        mDescription = itemView.findViewById(R.id.description);
        mUser = itemView.findViewById(R.id.user_name);
        mInstrument = itemView.findViewById(R.id.instrument);
        mDateCreated = itemView.findViewById(R.id.date_created);
        mThumbnail = itemView.findViewById(R.id.thumbnail);
        // Click Listener
        mListener = listener;
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        mThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onThumbnailClick(v, getAdapterPosition());
            }
        });
    }

    public void bind(@NonNull Fret fret) {
//        Log.d(TAG, "HELLO: " + fret.getName());
        setName(fret.getName());
        setDescription(fret.getDescription());
        setUser(getUser(fret.getUserId()));
        setThumbnail(getThumbnail(fret.getUserId()));
        setInstrument(fret.getInstrumentName(fret.getInstrument()));
        String d = formatDate(fret.getDatePublished());
//        Log.d(TAG, "HELLO date:"+d);
        setDateCreated(formatDate(fret.getDatePublished()));
    }

    String  getUser(String uid){
        if(uid.equals(FretApplication.getUID())){
//            Log.d(TAG, "HELLO: Current User");
            return FretApplication.getUserName();
        } else {
            // See if we have the profile in cache
            String userName = getUsernameFromCache(uid);
            if( userName.isEmpty()){
                downloadProfile(uid);
                return "Loading...";
            } else {
                return userName;
            }
        }
    }
    private Bitmap getThumbnail(final String uId) {
        File cacheFile = new File(cacheDir, uId + ImageUtils.THUMBNAIL_SUFFIX );
//        Log.d(TAG, "HELLO cachefile:"+cacheFile.getPath());
        if (cacheFile.exists()) {
            try {
//                Log.d(TAG, "HELLO thumbnail cachefile exists");
                return BitmapFactory.decodeFile(cacheFile.getPath());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                return null;
            }
        } else {
            Log.d(TAG, "HELLO thumbnail cachefile doesn't exist");
            downloadThumbnailPic(uId);
        }
        return null;
    }

    private void downloadThumbnailPic(String uId){
        StorageReference profilePicRef = FirebaseStorage.getInstance().getReference().child("profileThumbnail").child(uId);
        final File cacheFile = new File(cacheDir, uId + ImageUtils.THUMBNAIL_SUFFIX );
        profilePicRef.getFile(cacheFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                Log.d(TAG, "Thumbnail downloaded: "+cacheFile.getPath());
//                setThumbnail(ImageUtils.checkOrientation(cacheFile));
                setThumbnail(BitmapFactory.decodeFile(cacheFile.getPath()));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }

    private String getUsernameFromCache(String uid) {
        File cacheFile = new File(cacheDir, uid );
//        Log.d(TAG, "HELLO cachefile:"+cacheFile.getPath());
        if (cacheFile.exists()) {
            try {
//                Log.d(TAG, "HELLO cachefile exists");
                String userProfileJson = FileLoader.loadFile(cacheFile);
                return UserProfile.getUsername(userProfileJson);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                return "";
            }
        } else {
//            Log.d(TAG, "HELLO cachefile doesn't exist");
            return "";
        }
    }
    private void downloadProfile(final String uId) {
        // Get the Profile from the server
        DatabaseReference profileRef = FirebaseDatabase.getInstance().getReference().child("users").child(uId).child("userProfile");
        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                // Write profile (JSON) out to cache TODO update list
                writeProfileToCache(userProfile.toString(), uId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "OOPS " + databaseError.getMessage());
            }
        });
        return;
    }
    private void writeProfileToCache(String json, String uId) {
        final File cacheFile = new File(cacheDir, uId );
        FileWriterTask fileWriterTask = new FileWriterTask(cacheFile, json);
        fileWriterTask.setFileWrittenListener(new FileWriterTask.FileWrittenListener() {
            @Override
            public void OnFileWritten() {
//                Log.d(TAG, "HELLO file written to cache: " + cacheFile.getPath());
            }

            @Override
            public void OnError(String msg) {
                Log.d(TAG, "ERROR: "+msg);
            }
        });
        fileWriterTask.execute();
    }
    private String formatDate(long createDate){
        Date now = new Date();
        long nowL = now.getTime();
        long diff = nowL - createDate;
        long diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

        if(diffDays>365){
            // Over a year ago
            return "Ages ago...";
        }else if(diffDays>31){
            return simpleDF.format(new Date(createDate));
        }else if(diffDays>1){
            return diffDays+" days ago";
        }else if(diffDays == 1){
            return "1 day ago";
        }else{
            return "NEW!!";
        }
    }
    private void setName(@Nullable String name) {
        mName.setText(name);
    }
    private void setDescription(@Nullable String text) {
        if(!text.isEmpty()) {
            mDescription.setText("(" + text + ")");
        }
    }
    private void setUser(@Nullable String text) {
        mUser.setText(text);
    }
    private void setInstrument(@Nullable String text) {
        mInstrument.setText(text);
    }
    private void setDateCreated(@Nullable String text) {
        mDateCreated.setText(text);
    }
    private void setThumbnail(Bitmap bitmap){
        if(bitmap != null) {
            mThumbnail.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onClick(View v) {
        mListener.onClick(v, getAdapterPosition());
    }

    @Override
    public boolean onLongClick(View v) {
//        Log.d(TAG, "HELLO: onLongClick");
        mListener.onLongClick(v, getAdapterPosition());
        return true;
    }
}