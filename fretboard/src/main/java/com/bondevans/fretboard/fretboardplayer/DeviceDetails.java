package com.bondevans.fretboard.fretboardplayer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

/**
 * Current Device details
 */
public class DeviceDetails {
    private String manufacturer;
    private String model;
    private String version;
    private String device;
    private int oSVersion;
    private String hardware = "";

    DeviceDetails(Context context){
        String version="";
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        this.version = version;
        this.device = Build.DEVICE;
        this.oSVersion = Build.VERSION.SDK_INT;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.FROYO){
            this.hardware = Build.HARDWARE;
        }
        this.manufacturer=Build.MANUFACTURER;
        this.model = Build.MODEL;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getVersion() {
        return version;
    }

    public String getDevice() {
        return device;
    }

    public int getoSVersion() {
        return oSVersion;
    }

    public String getHardware() {
        return hardware;
    }
}
