package com.bondevans.frets.app;

import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.os.Handler;
import android.os.Looper;

import com.bondevans.frets.firebase.dao.UserProfile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.core.os.HandlerCompat;

/**
 * @author Paul Evans
 * @since 16/11/15
 *
 */
public class FretApplication extends android.app.Application {
    private static final String TAG = FretApplication.class.getSimpleName();
    private static String mUID = ""; // Unique User ID
    private static Context appContext;
    private static UserProfile userProfile;
    private static MidiDeviceInfo midiDeviceInfo;
    private static MidiDeviceInfo.PortInfo portInfo;
    public Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    public ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return appContext;
    }
    public static String getUserName() {
        return userProfile.getUsername();
    }
    public static String getUID() {
        return mUID;
    }
    public void setUser(String uID, UserProfile userProfile) {
        this.mUID = uID;
        this.userProfile = userProfile;
    }
    public static MidiDeviceInfo getMidiDeviceInfo() {
        return midiDeviceInfo;
    }

    public static void setMidiDeviceInfo(MidiDeviceInfo midiDeviceInfo) {
        FretApplication.midiDeviceInfo = midiDeviceInfo;
    }

    public static MidiDeviceInfo.PortInfo getPortInfo() {
        return portInfo;
    }

    public static void setPortInfo(MidiDeviceInfo.PortInfo portInfo) {
        FretApplication.portInfo = portInfo;
    }
}
