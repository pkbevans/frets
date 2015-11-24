package com.bondevans.fretboard.utils;

import com.bondevans.fretboard.utils.Log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Convenience class to write output to file on disk
 */
public class FileWriter {
    private static final String TAG = "FileWriter";

    public final static void writeFile(String fileName, String contents) throws IOException {
        Log.d(TAG, "Writing: " + fileName);
        byte[] utfHeader = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf};
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
        byte[] utf8 = contents.getBytes("UTF-8");
        // Don't add the UTF-8 header if its already there
        if (!isUTF8(utf8)) {
            // Always write out in UTF-8 adding 3 byte header
            out.write(utfHeader);
        }
        out.write(utf8);
        out.flush();
        out.close();
    }
    private static boolean isUTF8( byte[] buffer){
        // FIX 29/11/11 - Force close on creation of new set
        if (buffer.length>=3 && buffer[0] == (byte) 0xef && buffer[1] == (byte) 0xbb && buffer[2] == (byte) 0xbf){
//			Log.d(TAG, "IS UTF8");
            return true;
        }
        else{
//			Log.d(TAG, "NOT UTF8");
            return false;
        }
    }

}
