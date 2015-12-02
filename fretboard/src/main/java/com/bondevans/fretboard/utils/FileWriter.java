package com.bondevans.fretboard.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Convenience class to write output to file on disk
 */
public class FileWriter {
    private static final String TAG = "FileWriter";

    public static void writeFile(File file, String contents) throws IOException {
        Log.d(TAG, "Writing: " + file.getAbsolutePath());
        byte[] utfHeader = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf};
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
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
        return buffer.length >= 3 && buffer[0] == (byte) 0xef && buffer[1] == (byte) 0xbb && buffer[2] == (byte) 0xbf;
    }

    public static String loadFile(File file) throws Exception{
        StringBuilder sb = new StringBuilder();

            int length = 2048, bytesRead=0;
            byte[] buffer = new byte[length];

            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file), length);
            length=2048;
            while( bytesRead>=0){
                if((bytesRead = buf.read(buffer, 0, length))>=0 ){
                    // Store the buffer in the buffer array
                    sb.append(new String(buffer, 0, bytesRead));
                }
            }
            buf.close();
            return new String(sb.toString().getBytes("UTF-8"));
    }
}
