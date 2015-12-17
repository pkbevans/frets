package com.bondevans.fretboard.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Convenience class to write output to file on disk
 */
public class FileLoader {
    private static final String TAG = FileLoader.class.getSimpleName();

    public static String loadFile(File file) throws Exception {
        StringBuilder sb = new StringBuilder();

        int length = 2048, bytesRead = 0;
        byte[] buffer = new byte[length];

        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file), length);
        length = 2048;
        while (bytesRead >= 0) {
            if ((bytesRead = buf.read(buffer, 0, length)) >= 0) {
                // Store the buffer in the buffer array
                sb.append(new String(buffer, 0, bytesRead));
            }
        }
        buf.close();
        Log.d(TAG, "Loaded " + sb.length() + " bytes");
        return new String(sb.toString().getBytes("UTF-8"));
    }
}
