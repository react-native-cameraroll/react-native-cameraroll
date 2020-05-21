/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reactnativecommunity.cameraroll;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.media.ExifInterface;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.module.annotations.ReactModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.annotation.Nullable;

/**
 * {@link NativeModule} that allows JS to interact with the photos and videos on the device (i.e.
 * {@link MediaStore.Images}).
 */
@ReactModule(name = CameraRollModule.NAME)
public class CameraRollModule extends ReactContextBaseJavaModule {

  public static final String NAME = "RNCCameraRoll";

  private static final String ERROR_UNABLE_TO_LOAD = "E_UNABLE_TO_LOAD";
  private static final String ERROR_UNABLE_TO_LOAD_PERMISSION = "E_UNABLE_TO_LOAD_PERMISSION";
  private static final String ERROR_UNABLE_TO_SAVE = "E_UNABLE_TO_SAVE";
  private static final String ERROR_UNABLE_TO_DELETE = "E_UNABLE_TO_DELETE";
  private static final String ERROR_UNABLE_TO_FILTER = "E_UNABLE_TO_FILTER";

  private static final String ASSET_TYPE_PHOTOS = "Photos";
  private static final String ASSET_TYPE_VIDEOS = "Videos";
  private static final String ASSET_TYPE_ALL = "All";

  private static final String[] PROJECTION = {
    Images.Media._ID,
    Images.Media.MIME_TYPE,
    Images.Media.BUCKET_DISPLAY_NAME,
    Images.Media.DATE_TAKEN,
    MediaStore.MediaColumns.WIDTH,
    MediaStore.MediaColumns.HEIGHT,
    MediaStore.MediaColumns.SIZE,
    MediaStore.MediaColumns.DATA
  };

  private static final String SELECTION_BUCKET = Images.Media.BUCKET_DISPLAY_NAME + " = ?";
  private static final String SELECTION_DATE_TAKEN = Images.Media.DATE_TAKEN + " < ?";

  public CameraRollModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return NAME;
  }

  /**
   * Save an image to the gallery (i.e. {@link MediaStore.Images}). This copies the original file
   * from wherever it may be to the external storage pictures directory, so that it can be scanned
   * by the MediaScanner.
   *
   * @param uri the file:// URI of the image to save
   * @param promise to be resolved or rejected
   */
  @ReactMethod
  public void saveToCameraRoll(String uri, ReadableMap options, Promise promise) {
    new SaveToCameraRoll(getReactApplicationContext(), Uri.parse(uri), options, promise)
        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private static class SaveToCameraRoll extends GuardedAsyncTask<Void, Void> {

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
        File environment;
        if ("mov".equals(mOptions.getString("type"))) {
          environment = Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_MOVIES);
        } else {
          environment = Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_PICTURES);
        }
        File exportDir;
        if (!"".equals(mOptions.getString("album"))) {
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
  }

  /**
   * Get photos from {@link MediaStore.Images}, most recent first.
   *
   * @param params a map containing the following keys:
   *        <ul>
   *          <li>first (mandatory): a number representing the number of photos to fetch</li>
   *          <li>
   *            after (optional): a cursor that matches page_info[end_cursor] returned by a
   *            previous call to {@link #getPhotos}
   *          </li>
   *          <li>groupName (optional): an album name</li>
   *          <li>
   *            mimeType (optional): restrict returned images to a specific mimetype (e.g.
   *            image/jpeg)
   *          </li>
   *          <li>
   *            assetType (optional): chooses between either photos or videos from the camera roll.
   *            Valid values are "Photos" or "Videos". Defaults to photos.
   *          </li>
   *        </ul>
   * @param promise the Promise to be resolved when the photos are loaded; for a format of the
   *        parameters passed to this callback, see {@code getPhotosReturnChecker} in CameraRoll.js
   */
  @ReactMethod
  public void getPhotos(final ReadableMap params, final Promise promise) {
    int first = params.getInt("first");
    String after = params.hasKey("after") ? params.getString("after") : null;
    String groupName = params.hasKey("groupName") ? params.getString("groupName") : null;
    String assetType = params.hasKey("assetType") ? params.getString("assetType") : ASSET_TYPE_PHOTOS;
    long fromTime = params.hasKey("fromTime") ? (long) params.getDouble("fromTime") : 0;
    long toTime = params.hasKey("toTime") ? (long) params.getDouble("toTime") : 0;
    ReadableArray mimeTypes = params.hasKey("mimeTypes")
        ? params.getArray("mimeTypes")
        : null;
    ReadableArray include = params.hasKey("include") ? params.getArray("include") : null;

    new GetMediaTask(
          getReactApplicationContext(),
          first,
          after,
          groupName,
          mimeTypes,
          assetType,
          fromTime,
          toTime,
          promise)
          .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private static class GetMediaTask extends GuardedAsyncTask<Void, Void> {
    private final Context mContext;
    private final int mFirst;
    private final @Nullable String mAfter;
    private final @Nullable String mGroupName;
    private final @Nullable ReadableArray mMimeTypes;
    private final Promise mPromise;
    private final String mAssetType;
    private final long mFromTime;
    private final long mToTime;
    private final @Nullable ReadableArray mInclude;

    private GetMediaTask(
        ReactContext context,
        int first,
        @Nullable String after,
        @Nullable String groupName,
        @Nullable ReadableArray mimeTypes,
        String assetType,
        long fromTime,
        long toTime,
        @Nullable ReadableArray include,
        Promise promise) {
      super(context);
      mContext = context;
      mFirst = first;
      mAfter = after;
      mGroupName = groupName;
      mMimeTypes = mimeTypes;
      mPromise = promise;
      mAssetType = assetType;
      mFromTime = fromTime;
      mToTime = toTime;
      mInclude = include;
    }

    private boolean includeArrayContains(String value) {
      if (mInclude == null) {
        return false;
      }

      for (int i = 0; i < mInclude.size(); i++) {
        if (mInclude.getString(i) == value) {
          return true;
        }
      }
      return false;
    }

    @Override
    protected void doInBackgroundGuarded(Void... params) {
      StringBuilder selection = new StringBuilder("1");
      List<String> selectionArgs = new ArrayList<>();
      if (!TextUtils.isEmpty(mGroupName)) {
        selection.append(" AND " + SELECTION_BUCKET);
        selectionArgs.add(mGroupName);
      }

      if (mAssetType.equals(ASSET_TYPE_PHOTOS)) {
        selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
          + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
      } else if (mAssetType.equals(ASSET_TYPE_VIDEOS)) {
        selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
          + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
      } else if (mAssetType.equals(ASSET_TYPE_ALL)) {
        selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " IN ("
          + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ","
          + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + ")");
      } else {
        mPromise.reject(
          ERROR_UNABLE_TO_FILTER,
          "Invalid filter option: '" + mAssetType + "'. Expected one of '"
            + ASSET_TYPE_PHOTOS + "', '" + ASSET_TYPE_VIDEOS + "' or '" + ASSET_TYPE_ALL + "'."
        );
        return;
      }


      if (mMimeTypes != null && mMimeTypes.size() > 0) {
        selection.append(" AND " + Images.Media.MIME_TYPE + " IN (");
        for (int i = 0; i < mMimeTypes.size(); i++) {
          selection.append("?,");
          selectionArgs.add(mMimeTypes.getString(i));
        }
        selection.replace(selection.length() - 1, selection.length(), ")");
      }

      if (mFromTime > 0) {
        selection.append(" AND " + Images.Media.DATE_TAKEN + " > ?");
        selectionArgs.add(mFromTime + "");
      }
      if (mToTime > 0) {
        selection.append(" AND " + Images.Media.DATE_TAKEN + " <= ?");
        selectionArgs.add(mToTime + "");
      }

      WritableMap response = new WritableNativeMap();
      ContentResolver resolver = mContext.getContentResolver();

      try {
        // set LIMIT to first + 1 so that we know how to populate page_info
        String limit = "limit=" + (mFirst + 1);

        if (!TextUtils.isEmpty(mAfter)) {
          limit = "limit=" + mAfter + "," + (mFirst + 1);
        }

        Cursor media = resolver.query(
            MediaStore.Files.getContentUri("external").buildUpon().encodedQuery(limit).build(),
            PROJECTION,
            selection.toString(),
            selectionArgs.toArray(new String[selectionArgs.size()]),
            Images.Media.DATE_ADDED + " DESC, " + Images.Media.DATE_MODIFIED + " DESC");
        if (media == null) {
          mPromise.reject(ERROR_UNABLE_TO_LOAD, "Could not get media");
        } else {
          try {
            putEdges(resolver, media, response, mFirst);
            putPageInfo(media, response, mFirst, !TextUtils.isEmpty(mAfter) ? Integer.parseInt(mAfter) : 0);
          } finally {
            media.close();
            mPromise.resolve(response);
          }
        }
      } catch (SecurityException e) {
        mPromise.reject(
            ERROR_UNABLE_TO_LOAD_PERMISSION,
            "Could not get media: need READ_EXTERNAL_STORAGE permission",
            e);
      }
    }
  }

  @ReactMethod
  public void getAlbums(final ReadableMap params, final Promise promise) {
    String assetType = params.hasKey("assetType") ? params.getString("assetType") : ASSET_TYPE_ALL;
    StringBuilder selection = new StringBuilder("1");
    List<String> selectionArgs = new ArrayList<>();
    if (assetType.equals(ASSET_TYPE_PHOTOS)) {
      selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
              + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
    } else if (assetType.equals(ASSET_TYPE_VIDEOS)) {
      selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
              + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
    } else if (assetType.equals(ASSET_TYPE_ALL)) {
      selection.append(" AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " IN ("
              + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ","
              + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + ")");
    } else {
      promise.reject(
              ERROR_UNABLE_TO_FILTER,
              "Invalid filter option: '" + assetType + "'. Expected one of '"
                      + ASSET_TYPE_PHOTOS + "', '" + ASSET_TYPE_VIDEOS + "' or '" + ASSET_TYPE_ALL + "'."
      );
      return;
    }

    final String[] projection = {MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME};

    try {
      Cursor media = getReactApplicationContext().getContentResolver().query(
              MediaStore.Files.getContentUri("external"),
              projection,
              selection.toString(),
              selectionArgs.toArray(new String[selectionArgs.size()]),
              null);
      if (media == null) {
        promise.reject(ERROR_UNABLE_TO_LOAD, "Could not get media");
      } else {
        WritableArray response = new WritableNativeArray();
        try {
          if (media.moveToFirst()) {
            Map<String, Integer> albums = new HashMap<>();
            do {
              String albumName = media.getString(media.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
              if (albumName != null) {
                Integer albumCount = albums.get(albumName);
                if (albumCount == null) {
                  albums.put(albumName, 1);
                } else {
                  albums.put(albumName, albumCount + 1);
                }
              }
            } while (media.moveToNext());

            for (Map.Entry<String, Integer> albumEntry : albums.entrySet()) {
              WritableMap album = new WritableNativeMap();
              album.putString("title", albumEntry.getKey());
              album.putInt("count", albumEntry.getValue());
              response.pushMap(album);
            }
          }
        } finally {
          media.close();
          promise.resolve(response);
        }
      }
    } catch (Exception e) {
      promise.reject(ERROR_UNABLE_TO_LOAD, "Could not get media", e);
    }
  }

  private static void putPageInfo(Cursor media, WritableMap response, int limit, int offset) {
    WritableMap pageInfo = new WritableNativeMap();
    pageInfo.putBoolean("has_next_page", limit < media.getCount());
    if (limit < media.getCount()) {
      pageInfo.putString(
        "end_cursor",
        Integer.toString(offset + limit)
      );
    }
    response.putMap("page_info", pageInfo);
  }

  private static void putEdges(
      ContentResolver resolver,
      Cursor media,
      WritableMap response,
      int limit) {
    WritableArray edges = new WritableNativeArray();
    media.moveToFirst();
    int idIndex = media.getColumnIndex(Images.Media._ID);
    int mimeTypeIndex = media.getColumnIndex(Images.Media.MIME_TYPE);
    int groupNameIndex = media.getColumnIndex(Images.Media.BUCKET_DISPLAY_NAME);
    int dateTakenIndex = media.getColumnIndex(Images.Media.DATE_TAKEN);
    int widthIndex = media.getColumnIndex(MediaStore.MediaColumns.WIDTH);
    int heightIndex = media.getColumnIndex(MediaStore.MediaColumns.HEIGHT);
    int sizeIndex = media.getColumnIndex(MediaStore.MediaColumns.SIZE);
    int dataIndex = media.getColumnIndex(MediaStore.MediaColumns.DATA);

    boolean shouldGetLocationInfo = includeArrayContains("location");

    for (int i = 0; i < limit && !media.isAfterLast(); i++) {
      WritableMap edge = new WritableNativeMap();
      WritableMap node = new WritableNativeMap();
      boolean imageInfoSuccess =
          putImageInfo(resolver, media, node, idIndex, widthIndex, heightIndex, sizeIndex, dataIndex, mimeTypeIndex);
      if (imageInfoSuccess) {
        putBasicNodeInfo(media, node, mimeTypeIndex, groupNameIndex, dateTakenIndex);
        if (shouldGetLocationInfo) {
          putLocationInfo(media, node, dataIndex);
        }

        edge.putMap("node", node);
        edges.pushMap(edge);
      } else {
        // we skipped an image because we couldn't get its details (e.g. width/height), so we
        // decrement i in order to correctly reach the limit, if the cursor has enough rows
        i--;
      }
      media.moveToNext();
    }
    response.putArray("edges", edges);
  }

  private static void putBasicNodeInfo(
      Cursor media,
      WritableMap node,
      int mimeTypeIndex,
      int groupNameIndex,
      int dateTakenIndex) {
    node.putString("type", media.getString(mimeTypeIndex));
    node.putString("group_name", media.getString(groupNameIndex));
    node.putDouble("timestamp", media.getLong(dateTakenIndex) / 1000d);
  }

  private static boolean putImageInfo(
      ContentResolver resolver,
      Cursor media,
      WritableMap node,
      int idIndex,
      int widthIndex,
      int heightIndex,
      int sizeIndex,
      int dataIndex,
      int mimeTypeIndex) {
    WritableMap image = new WritableNativeMap();
    Uri photoUri = Uri.parse("file://" + media.getString(dataIndex));
    File file = new File(media.getString(dataIndex));
    String strFileName = file.getName();
    image.putString("uri", photoUri.toString());
    image.putString("filename", strFileName);
    float width = media.getInt(widthIndex);
    float height = media.getInt(heightIndex);
    long fileSize = media.getLong(sizeIndex);

    String mimeType = media.getString(mimeTypeIndex);

    if (mimeType != null
        && mimeType.startsWith("video")) {
      try {
        AssetFileDescriptor photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(photoDescriptor.getFileDescriptor());

        try {
          if (width <= 0 || height <= 0) {
            width =
                Integer.parseInt(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            height =
                Integer.parseInt(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
          }
          int timeInMillisec =
              Integer.parseInt(
                  retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
          int playableDuration = timeInMillisec / 1000;
          image.putInt("playableDuration", playableDuration);
        } catch (NumberFormatException e) {
          FLog.e(
              ReactConstants.TAG,
              "Number format exception occurred while trying to fetch video metadata for "
                  + photoUri.toString(),
              e);
          return false;
        } finally {
          retriever.release();
          photoDescriptor.close();
        }
      } catch (Exception e) {
        FLog.e(ReactConstants.TAG, "Could not get video metadata for " + photoUri.toString(), e);
        return false;
      }
    }

    if (width <= 0 || height <= 0) {
      try {
        AssetFileDescriptor photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
        BitmapFactory.Options options = new BitmapFactory.Options();
        // Set inJustDecodeBounds to true so we don't actually load the Bitmap, but only get its
        // dimensions instead.
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(photoDescriptor.getFileDescriptor(), null, options);
        width = options.outWidth;
        height = options.outHeight;
        photoDescriptor.close();
      } catch (IOException e) {
        FLog.e(ReactConstants.TAG, "Could not get width/height for " + photoUri.toString(), e);
        return false;
      }
    }
    image.putDouble("width", width);
    image.putDouble("height", height);
    image.putDouble("fileSize", fileSize);
    node.putMap("image", image);

    return true;
  }

  private static void putLocationInfo(
      Cursor media,
      WritableMap node,
      int dataIndex) {
      try {
        // location details are no longer indexed for privacy reasons using string Media.LATITUDE, Media.LONGITUDE
        // we manually obtain location metadata using ExifInterface#getLatLong(float[]).
        // ExifInterface is added in API level 5
        final ExifInterface exif = new ExifInterface(media.getString(dataIndex));
        float[] imageCoordinates = new float[2];
        boolean hasCoordinates = exif.getLatLong(imageCoordinates);
        if (hasCoordinates) {
          double longitude = imageCoordinates[1];
          double latitude = imageCoordinates[0];
          WritableMap location = new WritableNativeMap();
          location.putDouble("longitude", longitude);
          location.putDouble("latitude", latitude);
          node.putMap("location", location);
        }
      } catch (IOException e) {
        FLog.e(ReactConstants.TAG, "Could not read the metadata", e);
      }
  }

  /**
   * Delete a set of images.
   *
   * @param uris array of file:// URIs of the images to delete
   * @param promise to be resolved
   */
  @ReactMethod
  public void deletePhotos(ReadableArray uris, Promise promise) {
    if (uris.size() == 0) {
      promise.reject(ERROR_UNABLE_TO_DELETE, "Need at least one URI to delete");
    } else {
      new DeletePhotos(getReactApplicationContext(), uris, promise)
          .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private static class DeletePhotos extends GuardedAsyncTask<Void, Void> {

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
}
