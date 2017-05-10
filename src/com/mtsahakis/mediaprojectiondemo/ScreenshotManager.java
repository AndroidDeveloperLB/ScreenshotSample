package com.mtsahakis.mediaprojectiondemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjection.Callback;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class ScreenshotManager {
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    public static final ScreenshotManager INSTANCE = new ScreenshotManager();
    private Intent mIntent;

    private ScreenshotManager() {
    }

    public void requestScreenshotPermission(@NonNull Activity activity, int requestId) {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), requestId);
    }


    public void onActivityResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null)
            mIntent = data;
        else mIntent = null;
    }

    @UiThread
    public boolean takeScreenshot(@NonNull Context context) {
        if (mIntent == null)
            return false;
        final MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, mIntent);
        if (mediaProjection == null)
            return false;
        final int density = context.getResources().getDisplayMetrics().densityDpi;
        final Point windowSize = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(windowSize);
        final int width = windowSize.x, height = windowSize.y;
        final Context applicationContext = context.getApplicationContext();

        // start capture reader
        final ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        final VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(SCREENCAP_NAME, width, height, density, VIRTUAL_DISPLAY_FLAGS, imageReader.getSurface(), null, null);
        imageReader.setOnImageAvailableListener(new OnImageAvailableListener() {
            @Override
            public void onImageAvailable(final ImageReader reader) {
                Log.d("AppLog", "onImageAvailable");
                mediaProjection.stop();
                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(final Void... params) {
                        Image image = null;
                        Bitmap bitmap = null;
                        try {
                            image = reader.acquireLatestImage();
                            if (image != null) {
                                Plane[] planes = image.getPlanes();
                                ByteBuffer buffer = planes[0].getBuffer();
                                int pixelStride = planes[0].getPixelStride(), rowStride = planes[0].getRowStride(), rowPadding = rowStride - pixelStride * width;
                                bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Config.ARGB_8888);
                                bitmap.copyPixelsFromBuffer(buffer);
                                // fix the extra width from Image
                                Bitmap croppedBitmap;
                                croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, windowSize.x, windowSize.y);
                                if (croppedBitmap != bitmap)
                                    bitmap.recycle();
                                return croppedBitmap;
                            }
                        } catch (Exception e) {
                            if (bitmap != null)
                                bitmap.recycle();
                            e.printStackTrace();
                        }
                        if (image != null)
                            image.close();
                        reader.close();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(final Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        Log.d("AppLog", "Got bitmap?" + (bitmap != null));
                        Toast.makeText(applicationContext, "Got bitmap?" + (bitmap != null), Toast.LENGTH_SHORT).show();
                    }
                }.execute();
            }
        }, null);
        mediaProjection.registerCallback(new Callback() {
            @Override
            public void onStop() {
                super.onStop();
                if (virtualDisplay != null)
                    virtualDisplay.release();
                imageReader.setOnImageAvailableListener(null, null);
                mediaProjection.unregisterCallback(this);
            }
        }, null);
        return true;
    }
}
