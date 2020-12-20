package com.reactnativecommunity.cameraroll.operation;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.ReactConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class SaveToCameraRoll extends GuardedAsyncTask<Void, Void> {
    private static final String ERROR_UNABLE_TO_SAVE = "E_UNABLE_TO_SAVE";
    private static final String ERROR_UNABLE_TO_LOAD = "E_UNABLE_TO_LOAD";

    private final Context mContext;
    private final Uri mUri;
    private final Promise mPromise;
    private final ReadableMap mOptions;

    public SaveToCameraRoll(ReactContext context, Uri uri, ReadableMap options, Promise promise) {
        super(context);
        mContext = context;
        mUri = uri;
        mPromise = promise;
        mOptions = options;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
        File source = new File(mUri.getPath());
        FileChannel input = null, output = null;
        try {
            boolean isAlbumPresent = !"".equals(mOptions.getString("album"));
            File environment;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                manageAndroidQ(source);
                return;
            } else {
                environment = mContext.getExternalFilesDir(getEnvironmentForMimeType());
            }

            File exportDir;
            if (isAlbumPresent) {
                exportDir = new File(environment, mOptions.getString("album"));
                if (!exportDir.exists() && !exportDir.mkdirs()) {
                    mPromise.reject(ERROR_UNABLE_TO_LOAD, "Album Directory not created. Did you request WRITE_EXTERNAL_STORAGE?");
                    return;
                }
            } else {
                exportDir = environment;
            }

            if (!exportDir.isDirectory()) {
                mPromise.reject(ERROR_UNABLE_TO_LOAD, "External media storage directory not available");
                return;
            }
            File dest = new File(exportDir, source.getName());
            int n = 0;
            String fullSourceName = source.getName();
            String sourceName, sourceExt;
            if (fullSourceName.indexOf('.') >= 0) {
                sourceName = fullSourceName.substring(0, fullSourceName.lastIndexOf('.'));
                sourceExt = fullSourceName.substring(fullSourceName.lastIndexOf('.'));
            } else {
                sourceName = fullSourceName;
                sourceExt = "";
            }
            while (!dest.createNewFile()) {
                dest = new File(exportDir, sourceName + "_" + (n++) + sourceExt);
            }
            input = new FileInputStream(source).getChannel();
            output = new FileOutputStream(dest).getChannel();
            output.transferFrom(input, 0, input.size());
            input.close();
            output.close();

            MediaScannerConnection.scanFile(
                    mContext,
                    new String[]{dest.getAbsolutePath()},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            if (uri != null) {
                                mPromise.resolve(uri.toString());
                            } else {
                                mPromise.reject(ERROR_UNABLE_TO_SAVE, "Could not add image to gallery");
                            }
                        }
                    });
        } catch (IOException e) {
            mPromise.reject(e);
        } finally {
            if (input != null && input.isOpen()) {
                try {
                    input.close();
                } catch (IOException e) {
                    FLog.e(ReactConstants.TAG, "Could not close input channel", e);
                }
            }
            if (output != null && output.isOpen()) {
                try {
                    output.close();
                } catch (IOException e) {
                    FLog.e(ReactConstants.TAG, "Could not close output channel", e);
                }
            }
        }
    }

    private void manageAndroidQ(File source) throws IOException {
        FileChannel input = new FileInputStream(source).getChannel();
        String fullSourceName = source.getName();

        ContentResolver resolver = mContext.getContentResolver();
        Uri videoCollection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;;

        ContentValues contentDetails = new ContentValues();
        contentDetails.put(MediaStore.Video.Media.DISPLAY_NAME, fullSourceName);
        contentDetails.put(MediaStore.Video.Media.IS_PENDING, 1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String relativePath = Environment.DIRECTORY_MOVIES + File.separator + mContext.getPackageName();
            contentDetails.put(MediaStore.Video.Media.RELATIVE_PATH, relativePath);
        }

        Uri contentUri = resolver.insert(videoCollection, contentDetails);
        ParcelFileDescriptor parcel = resolver.openFileDescriptor(contentUri, "w");

        FileOutputStream out = new FileOutputStream(parcel.getFileDescriptor());
        FileChannel destination = out.getChannel();
        destination.transferFrom(input, 0, input.size());

        input = new FileInputStream(source).getChannel();
        destination.close();
        input.close();
        parcel.close();
        contentDetails.clear();
        contentDetails.put(MediaStore.Video.Media.IS_PENDING, 0);
        resolver.update(contentUri, contentDetails, null, null);

        mPromise.resolve(contentUri.toString());
    }

    /**
     * To get the file path from given a content URI use this method.
     * Be sure to set this property `android:requestLegacyExternalStorage="true"` to true
     * in your app's AndroidManifest.xml file.
     *
     * Android 11 and Android 10 with the above set in the application tag will work fine and will be
     * able to access the file's content through a direct file path and the File java API.
     *
     * @param context
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    private String getEnvironmentForMimeType() {
        String mimeType = getMimeTypeFromFile();

        if (mimeType.startsWith("image")) {
            return Environment.DIRECTORY_PICTURES;
        } else if (mimeType.startsWith("video")) {
            return Environment.DIRECTORY_MOVIES;
        } else if (mimeType.startsWith("audio")) {
            return Environment.DIRECTORY_MUSIC;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Environment.DIRECTORY_DOCUMENTS;
        } else {
            return Environment.DIRECTORY_DCIM;
        }
    }

    private String getMimeTypeFromFile() {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(mUri.getScheme())) {
            ContentResolver cr = mContext.getContentResolver();
            mimeType = cr.getType(mUri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mUri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }
}
