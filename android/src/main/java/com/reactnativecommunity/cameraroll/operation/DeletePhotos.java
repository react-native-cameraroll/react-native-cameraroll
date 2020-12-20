package com.reactnativecommunity.cameraroll.operation;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;

public class DeletePhotos extends GuardedAsyncTask<Void, Void> {

    private static final String ERROR_UNABLE_TO_DELETE = "E_UNABLE_TO_DELETE";

    private final Context mContext;
    private final ReadableArray mUris;
    private final Promise mPromise;

    public DeletePhotos(ReactContext context, ReadableArray uris, Promise promise) {
        super(context);
        mContext = context;
        mUris = uris;
        mPromise = promise;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
        ContentResolver resolver = mContext.getContentResolver();

        // Set up the projection (we only need the ID)
        String[] projection = { MediaStore.Images.Media._ID };

        // Match on the file path
        String innerWhere = "?";
        for (int i = 1; i < mUris.size(); i++) {
            innerWhere += ", ?";
        }

        String selection = MediaStore.Images.Media.DATA + " IN (" + innerWhere + ")";
        // Query for the ID of the media matching the file path
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] selectionArgs = new String[mUris.size()];
        for (int i = 0; i < mUris.size(); i++) {
            Uri uri = Uri.parse(mUris.getString(i));
            selectionArgs[i] = uri.getPath();
        }

        Cursor cursor = resolver.query(queryUri, projection, selection, selectionArgs, null);
        int deletedCount = 0;

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            if (resolver.delete(deleteUri, null, null) == 1) {
                deletedCount++;
            }
        }

        cursor.close();

        if (deletedCount == mUris.size()) {
            mPromise.resolve(true);
        } else {
            mPromise.reject(ERROR_UNABLE_TO_DELETE,
                    "Could not delete all media, only deleted " + deletedCount + " photos.");
        }
    }
}
