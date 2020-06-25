package com.bondevans.frets;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.bondevans.frets.app.FretApplication;
import com.bondevans.frets.utils.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.exifinterface.media.ExifInterface;

public class ImageUtils {
    private static final String TAG = ImageUtils.class.getSimpleName();
    public static final String THUMBNAIL_SUFFIX = "_thumbnail.bmp";
    static final File cacheDir = new File(FretApplication.getAppContext().getExternalCacheDir(),"" );

    public static Bitmap checkOrientation(File photo){
        Log.d(TAG, "HELLO - checkOrientation: "+photo);
        ExifInterface ei = null;
        try {
            ei = new ExifInterface(photo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        float angle=0;
        switch(orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                Log.d(TAG, "HELLO - checkOrientation: 90");
                angle = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                Log.d(TAG, "HELLO - checkOrientation: 180");
                angle = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                Log.d(TAG, "HELLO - checkOrientation: 270");
                angle = 270;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                Log.d(TAG, "HELLO - checkOrientation: NORMAL");
            default:
                Log.d(TAG, "HELLO - checkOrientation: UNDEFINED");
        }
        return rotateImage(BitmapFactory.decodeFile(photo.getPath()), angle);
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        Log.d(TAG, "HELLO rotateImage:"+angle);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
    public static void deleteThumbnailFromCache(String uid){
        File cacheFile = new File(cacheDir,uid+THUMBNAIL_SUFFIX);
        boolean z = cacheFile.exists();
        Log.d(TAG, "Deleting cachefile:"+cacheFile.getPath()+" EXISTS:"+(z?"TRUE":"FALSE"));
        boolean x = cacheFile.delete();
        Log.d(TAG, "Delete :"+ (x ?"SUCCESS":"FAIL"));
    }
    public static void writeThumbnailToCache(String uid, Bitmap bmp){
        File cacheFile = new File(cacheDir,uid+THUMBNAIL_SUFFIX);
        try (FileOutputStream out = new FileOutputStream(cacheFile.getPath())) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getCircularBitmap(Bitmap srcBitmap) {
        // Calculate the circular bitmap width with border
        int squareBitmapWidth = Math.min(srcBitmap.getWidth(), srcBitmap.getHeight());
        // Initialize a new instance of Bitmap
        Bitmap dstBitmap = Bitmap.createBitmap (
                squareBitmapWidth, // Width
                squareBitmapWidth, // Height
                Bitmap.Config.ARGB_8888 // Config
        );
        Canvas canvas = new Canvas(dstBitmap);
        // Initialize a new Paint instance
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, squareBitmapWidth, squareBitmapWidth);
        RectF rectF = new RectF(rect);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // Calculate the left and top of copied bitmap
        float left = (squareBitmapWidth-srcBitmap.getWidth())/2;
        float top = (squareBitmapWidth-srcBitmap.getHeight())/2;
        canvas.drawBitmap(srcBitmap, left, top, paint);
        // Free the native object associated with this bitmap.
        srcBitmap.recycle();
        // Return the circular bitmap
        return dstBitmap;
    }
}