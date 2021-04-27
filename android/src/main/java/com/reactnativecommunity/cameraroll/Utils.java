package com.reactnativecommunity.cameraroll;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import javax.annotation.Nullable;

public class Utils {

  // From https://stackoverflow.com/a/64359655/1377145
  @Nullable
  public static String getFilePathFromContentUri(Context context, Uri contentUri) {
    ContentResolver contentResolver = context.getContentResolver();
    Cursor cursor = contentResolver.query(contentUri, null, null, null, null);
    if (cursor == null) {
      return null;
    }
    cursor.moveToFirst();
    String document_id = cursor.getString(0);
    document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
    cursor.close();

    cursor = contentResolver.query(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
    if (cursor == null) {
      return null;
    }
    cursor.moveToFirst();
    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
    cursor.close();
    return path;
  }

}
