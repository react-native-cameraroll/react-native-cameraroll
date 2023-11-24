/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reactnativecommunity.cameraroll;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.media.ExifInterface;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.module.annotations.ReactModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * {@link NativeModule} that allows JS to interact with the photos and videos on the device (i.e.
 * {@link MediaStore.Images}).
 */
@ReactModule(name = CameraRollModule.NAME)
public class CameraRollModule extends NativeCameraRollModuleSpec {

  public static final String NAME = "RNCCameraRoll";

  private static final String ERROR_UNABLE_TO_LOAD = "E_UNABLE_TO_LOAD";
  private static final String ERROR_UNABLE_TO_LOAD_PERMISSION = "E_UNABLE_TO_LOAD_PERMISSION";
  private static final String ERROR_UNABLE_TO_SAVE = "E_UNABLE_TO_SAVE";
  private static final String ERROR_UNABLE_TO_DELETE = "E_UNABLE_TO_DELETE";
  private static final String ERROR_UNABLE_TO_FILTER = "E_UNABLE_TO_FILTER";

  private static final String ASSET_TYPE_PHOTOS = "Photos";
  private static final String ASSET_TYPE_VIDEOS = "Videos";
  private static final String ASSET_TYPE_ALL = "All";

  private static final String INCLUDE_FILENAME = "filename";
  private static final String INCLUDE_FILE_SIZE = "fileSize";
  private static final String INCLUDE_FILE_EXTENSION = "fileExtension";
  private static final String INCLUDE_LOCATION = "location";
  private static final String INCLUDE_IMAGE_SIZE = "imageSize";
  private static final String INCLUDE_PLAYABLE_DURATION = "playableDuration";
  private static final String INCLUDE_ORIENTATION = "orientation";
  private static final String INCLUDE_ALBUMS = "albums";

  private static final String[] PROJECTION = {
          Images.Media._ID,
          Images.Media.MIME_TYPE,
          Images.Media.BUCKET_DISPLAY_NAME,
          Images.Media.DATE_TAKEN,
          MediaStore.MediaColumns.DATE_ADDED,
          MediaStore.MediaColumns.DATE_MODIFIED,
          MediaStore.MediaColumns.WIDTH,
          MediaStore.MediaColumns.HEIGHT,
          MediaStore.MediaColumns.SIZE,
          MediaStore.MediaColumns.DATA,
          MediaStore.MediaColumns.ORIENTATION,
  };

  private static final String SELECTION_BUCKET = Images.Media.BUCKET_DISPLAY_NAME + " = ?";

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
   * @param uri     the file:// URI of the image to save
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
      FileInputStream input = null;
      OutputStream output = null;

      String mimeType = Utils.getMimeType(mUri.toString());
      Boolean isVideo = mimeType != null && mimeType.contains("video");

      try {
        String album = mOptions.getString("album");
        boolean isAlbumPresent = !TextUtils.isEmpty(album);

        // Android Q and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          ContentValues mediaDetails = new ContentValues();
          if (isAlbumPresent) {
            String relativePath = Environment.DIRECTORY_DCIM + File.separator + album;
            mediaDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
          }
          mediaDetails.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
          mediaDetails.put(Images.Media.DISPLAY_NAME, source.getName());
          mediaDetails.put(Images.Media.IS_PENDING, 1);
          ContentResolver resolver = mContext.getContentResolver();
          Uri mediaContentUri = isVideo
                  ? resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaDetails)
                  : resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaDetails);
          if (mediaContentUri == null) {
            mPromise.reject(ERROR_UNABLE_TO_LOAD, "ContentResolver#insert() returns null, insert failed");
          }
          output = resolver.openOutputStream(mediaContentUri);
          input = new FileInputStream(source);
          FileUtils.copy(input, output);
          mediaDetails.clear();
          mediaDetails.put(Images.Media.IS_PENDING, 0);
          resolver.update(mediaContentUri, mediaDetails, null, null);
          mPromise.resolve(mediaContentUri.toString());
        } else {
          final File environment;
          // Media is not saved into an album when using Environment.DIRECTORY_DCIM.
          if (isAlbumPresent) {
            if ("video".equals(mOptions.getString("type"))) {
              environment = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            } else {
              environment = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            }
          } else {
            environment = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
          }
          File exportDir;
          if (isAlbumPresent) {
            exportDir = new File(environment, album);
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
          input = new FileInputStream(source);
          output = new FileOutputStream(dest);
          ((FileOutputStream) output).getChannel()
                  .transferFrom(input.getChannel(), 0, input.getChannel().size());
          input.close();
          output.close();

          MediaScannerConnection.scanFile(
                  mContext,
                  new String[]{dest.getAbsolutePath()},
                  null,
                  (path, uri) -> {
                    if (uri != null) {
                      mPromise.resolve(uri.toString());
                    } else {
                      mPromise.reject(ERROR_UNABLE_TO_SAVE, "Could not add image to gallery");
                    }
                  });
        }
      } catch (IOException e) {
        mPromise.reject(e);
      } finally {
        if (input != null) {
          try {
            input.close();
          } catch (IOException e) {
            FLog.e(ReactConstants.TAG, "Could not close input channel", e);
          }
        }
        if (output != null) {
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
   * @param params  a map containing the following keys:
   *                <ul>
   *                  <li>first (mandatory): a number representing the number of photos to fetch</li>
   *                  <li>
   *                    after (optional): a cursor that matches page_info[end_cursor] returned by a
   *                    previous call to {@link #getPhotos}
   *                  </li>
   *                  <li>groupName (optional): an album name</li>
   *                  <li>
   *                    mimeType (optional): restrict returned images to a specific mimetype (e.g.
   *                    image/jpeg)
   *                  </li>
   *                  <li>
   *                    assetType (optional): chooses between either photos or videos from the camera roll.
   *                    Valid values are "Photos" or "Videos". Defaults to photos.
   *                  </li>
   *                </ul>
   * @param promise the Promise to be resolved when the photos are loaded; for a format of the
   *                parameters passed to this callback, see {@code getPhotosReturnChecker} in CameraRoll.js
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
            include,
            promise)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private static class GetMediaTask extends GuardedAsyncTask<Void, Void> {
    private final Context mContext;
    private final int mFirst;
    private final @Nullable
    String mAfter;
    private final @Nullable
    String mGroupName;
    private final @Nullable
    ReadableArray mMimeTypes;
    private final Promise mPromise;
    private final String mAssetType;
    private final long mFromTime;
    private final long mToTime;
    private final Set<String> mInclude;

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
      mInclude = createSetFromIncludeArray(include);
    }

    private static Set<String> createSetFromIncludeArray(@Nullable ReadableArray includeArray) {
      Set<String> includeSet = new HashSet<>();

      if (includeArray == null) {
        return includeSet;
      }

      for (int i = 0; i < includeArray.size(); i++) {
        @Nullable String includeItem = includeArray.getString(i);
        if (includeItem != null) {
          includeSet.add(includeItem);
        }
      }

      return includeSet;
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
        long addedDate = mFromTime / 1000;
        selection.append(" AND (" + Images.Media.DATE_TAKEN + " > ? OR ( " + Images.Media.DATE_TAKEN
                + " IS NULL AND " + Images.Media.DATE_ADDED + "> ? ))");
        selectionArgs.add(mFromTime + "");
        selectionArgs.add(addedDate + "");
      }
      if (mToTime > 0) {
        long addedDate = mToTime / 1000;
        selection.append(" AND (" + Images.Media.DATE_TAKEN + " <= ? OR ( " + Images.Media.DATE_TAKEN
                + " IS NULL AND " + Images.Media.DATE_ADDED + " <= ? ))");
        selectionArgs.add(mToTime + "");
        selectionArgs.add(addedDate + "");
      }

      WritableMap response = new WritableNativeMap();
      ContentResolver resolver = mContext.getContentResolver();

      try {
        Cursor media;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          Bundle bundle = new Bundle();
          bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.toString());
          bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                  selectionArgs.toArray(new String[selectionArgs.size()]));
          bundle.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, Images.Media.DATE_ADDED + " DESC, " + Images.Media.DATE_MODIFIED + " DESC");
          bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, mFirst + 1);
          if (!TextUtils.isEmpty(mAfter)) {
            bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, Integer.parseInt(mAfter));
          }
          media = resolver.query(
                  MediaStore.Files.getContentUri("external"),
                  PROJECTION,
                  bundle,
                  null);
        } else {
          // set LIMIT to first + 1 so that we know how to populate page_info
          String limit = "limit=" + (mFirst + 1);
          if (!TextUtils.isEmpty(mAfter)) {
            limit = "limit=" + mAfter + "," + (mFirst + 1);
          }
          media = resolver.query(
                  MediaStore.Files.getContentUri("external").buildUpon().encodedQuery(limit).build(),
                  PROJECTION,
                  selection.toString(),
                  selectionArgs.toArray(new String[selectionArgs.size()]),
                  Images.Media.DATE_ADDED + " DESC, " + Images.Media.DATE_MODIFIED + " DESC");
        }

        if (media == null) {
          mPromise.reject(ERROR_UNABLE_TO_LOAD, "Could not get media");
        } else {
          try {
            putEdges(resolver, media, response, mFirst, mInclude);
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
              int column = media.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);
              if (column < 0) {
                throw new IndexOutOfBoundsException();
              }
              String albumName = media.getString(column);
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
          int limit,
          Set<String> include) {
    WritableArray edges = new WritableNativeArray();
    media.moveToFirst();
    int idIndex = media.getColumnIndex(Images.Media._ID);
    int mimeTypeIndex = media.getColumnIndex(Images.Media.MIME_TYPE);
    int groupNameIndex = media.getColumnIndex(Images.Media.BUCKET_DISPLAY_NAME);
    int dateTakenIndex = media.getColumnIndex(Images.Media.DATE_TAKEN);
    int dateAddedIndex = media.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);
    int dateModifiedIndex = media.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);
    int widthIndex = media.getColumnIndex(MediaStore.MediaColumns.WIDTH);
    int heightIndex = media.getColumnIndex(MediaStore.MediaColumns.HEIGHT);
    int sizeIndex = media.getColumnIndex(MediaStore.MediaColumns.SIZE);
    int dataIndex = media.getColumnIndex(MediaStore.MediaColumns.DATA);
    int orientationIndex = media.getColumnIndex(MediaStore.MediaColumns.ORIENTATION);

    boolean includeLocation = include.contains(INCLUDE_LOCATION);
    boolean includeFilename = include.contains(INCLUDE_FILENAME);
    boolean includeFileSize = include.contains(INCLUDE_FILE_SIZE);
    boolean includeFileExtension = include.contains(INCLUDE_FILE_EXTENSION);
    boolean includeImageSize = include.contains(INCLUDE_IMAGE_SIZE);
    boolean includePlayableDuration = include.contains(INCLUDE_PLAYABLE_DURATION);
    boolean includeOrientation = include.contains(INCLUDE_ORIENTATION);
    boolean includeAlbums = include.contains(INCLUDE_ALBUMS);

    for (int i = 0; i < limit && !media.isAfterLast(); i++) {
      WritableMap edge = new WritableNativeMap();
      WritableMap node = new WritableNativeMap();
      boolean imageInfoSuccess =
              putImageInfo(resolver, media, node, widthIndex, heightIndex, sizeIndex, dataIndex, orientationIndex,
                      mimeTypeIndex, includeFilename, includeFileSize, includeFileExtension, includeImageSize,
                      includePlayableDuration, includeOrientation);
      if (imageInfoSuccess) {
        putBasicNodeInfo(media, node, idIndex, mimeTypeIndex, groupNameIndex, dateTakenIndex, dateAddedIndex, dateModifiedIndex, includeAlbums);
        putLocationInfo(media, node, dataIndex, includeLocation, mimeTypeIndex, resolver);

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
          int idIndex,
          int mimeTypeIndex,
          int groupNameIndex,
          int dateTakenIndex,
          int dateAddedIndex,
          int dateModifiedIndex,
          boolean includeAlbums) {
    node.putString("id", Long.toString(media.getLong(idIndex)));
    node.putString("type", media.getString(mimeTypeIndex));
    WritableArray subTypes = Arguments.createArray();
    node.putArray("subTypes", subTypes);
    WritableArray group_name = Arguments.createArray();
    if (includeAlbums) {
      group_name.pushString(media.getString(groupNameIndex));
    }
    node.putArray("group_name", group_name);
    long dateTaken = media.getLong(dateTakenIndex);
    if (dateTaken == 0L) {
      //date added is in seconds, date taken in milliseconds, thus the multiplication
      dateTaken = media.getLong(dateAddedIndex) * 1000;
    }
    node.putDouble("timestamp", dateTaken / 1000d);
    node.putDouble("modificationTimestamp", media.getLong(dateModifiedIndex));
  }

  /**
   * @return Whether we successfully fetched all the information about the image that we were asked
   * to include
   */
  private static boolean putImageInfo(
          ContentResolver resolver,
          Cursor media,
          WritableMap node,
          int widthIndex,
          int heightIndex,
          int sizeIndex,
          int dataIndex,
          int orientationIndex,
          int mimeTypeIndex,
          boolean includeFilename,
          boolean includeFileSize,
          boolean includeFileExtension,
          boolean includeImageSize,
          boolean includePlayableDuration,
          boolean includeOrientation) {
    WritableMap image = new WritableNativeMap();
    Uri photoUri = Uri.parse("file://" + media.getString(dataIndex));
    image.putString("uri", photoUri.toString());
    String mimeType = media.getString(mimeTypeIndex);

    boolean isVideo = mimeType != null && mimeType.startsWith("video");
    boolean putImageSizeSuccess = putImageSize(resolver, media, image, widthIndex, heightIndex, orientationIndex,
            photoUri, isVideo, includeImageSize);
    boolean putPlayableDurationSuccess = putPlayableDuration(resolver, image, photoUri, isVideo,
            includePlayableDuration);

    if (includeFilename) {
      File file = new File(media.getString(dataIndex));
      String strFileName = file.getName();
      image.putString("filename", strFileName);
    } else {
      image.putNull("filename");
    }

    if (includeFileSize) {
      image.putDouble("fileSize", media.getLong(sizeIndex));
    } else {
      image.putNull("fileSize");
    }

    if (includeFileExtension) {
      image.putString("extension", Utils.getExtension(mimeType));
    } else {
      image.putNull("extension");
    }

    if (includeOrientation) {
      if(media.isNull(orientationIndex)) {
        image.putInt("orientation", media.getInt(orientationIndex));
      } else {
        image.putInt("orientation", 0);
      }
    } else {
      image.putNull("orientation");
    }

    node.putMap("image", image);
    return putImageSizeSuccess && putPlayableDurationSuccess;
  }

  /**
   * @return Whether we succeeded in fetching and putting the playableDuration
   */
  private static boolean putPlayableDuration(
          ContentResolver resolver,
          WritableMap image,
          Uri photoUri,
          boolean isVideo,
          boolean includePlayableDuration) {
    image.putNull("playableDuration");

    if (!includePlayableDuration || !isVideo) {
      return true;
    }

    boolean success = true;
    @Nullable Integer playableDuration = null;
    @Nullable AssetFileDescriptor photoDescriptor = null;
    try {
      photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
    } catch (FileNotFoundException e) {
      success = false;
      FLog.e(ReactConstants.TAG, "Could not open asset file " + photoUri.toString(), e);
    }

    if (photoDescriptor != null) {
      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      try {
        retriever.setDataSource(photoDescriptor.getFileDescriptor());
      } catch (RuntimeException e) {
        // Do nothing. We can't handle this, and this is usually a system problem
      }
      try {
        int timeInMillisecond = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        playableDuration = timeInMillisecond / 1000;
      } catch (NumberFormatException e) {
        success = false;
        FLog.e(
                ReactConstants.TAG,
                "Number format exception occurred while trying to fetch video metadata for "
                        + photoUri.toString(),
                e);
      }
      try {
        retriever.release();
      } catch (Exception e) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
        // Do nothing. We can't handle this, and this is usually a system problem
      }
    }

    if (photoDescriptor != null) {
      try {
        photoDescriptor.close();
      } catch (IOException e) {
        // Do nothing. We can't handle this, and this is usually a system problem
      }
    }

    if (playableDuration != null) {
      image.putInt("playableDuration", playableDuration);
    }

    return success;
  }

  private static boolean putImageSize(
          ContentResolver resolver,
          Cursor media,
          WritableMap image,
          int widthIndex,
          int heightIndex,
          int orientationIndex,
          Uri photoUri,
          boolean isVideo,
          boolean includeImageSize) {
    image.putNull("width");
    image.putNull("height");

    if (!includeImageSize) {
      return true;
    }

    boolean success = true;

    int width = media.getInt(widthIndex);
    int height = media.getInt(heightIndex);

    /* If the columns don't contain the size information, read the media file */
    if (width <= 0 || height <= 0) {
      @Nullable AssetFileDescriptor mediaDescriptor = null;
      try {
        mediaDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
      } catch (FileNotFoundException e) {
        success = false;
        FLog.e(ReactConstants.TAG, "Could not open asset file " + photoUri.toString(), e);
      }
      if (mediaDescriptor != null) {
        if (isVideo) {
          MediaMetadataRetriever retriever = new MediaMetadataRetriever();
          try {
            retriever.setDataSource(mediaDescriptor.getFileDescriptor());
          } catch (RuntimeException e) {
            // Do nothing. We can't handle this, and this is usually a system problem
          }
          try {
            width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
          } catch (NumberFormatException e) {
            success = false;
            FLog.e(
                    ReactConstants.TAG,
                    "Number format exception occurred while trying to fetch video metadata for "
                            + photoUri.toString(),
                    e);
          }
          try {
            retriever.release();
          } catch (Exception e) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
            // Do nothing. We can't handle this, and this is usually a system problem
          }
        } else {
          BitmapFactory.Options options = new BitmapFactory.Options();
          // Set inJustDecodeBounds to true so we don't actually load the Bitmap, but only get its
          // dimensions instead.
          options.inJustDecodeBounds = true;
          BitmapFactory.decodeFileDescriptor(mediaDescriptor.getFileDescriptor(), null, options);
          width = options.outWidth;
          height = options.outHeight;
        }

        try {
          mediaDescriptor.close();
        } catch (IOException e) {
          FLog.e(
                  ReactConstants.TAG,
                  "Can't close media descriptor "
                          + photoUri.toString(),
                  e);
        }
      }

    }

    if(!media.isNull(orientationIndex)) {
      int orientation = media.getInt(orientationIndex);
      if (orientation >= 0 && orientation % 180 != 0) {
        int temp = width;
        width = height;
        height = temp;
      }
    }

    image.putInt("width", width);
    image.putInt("height", height);
    return success;
  }

  private static void putLocationInfo(
          Cursor media,
          WritableMap node,
          int dataIndex,
          boolean includeLocation,
          int mimeTypeIndex,
          ContentResolver resolver) {
    node.putNull("location");

    if (!includeLocation) {
      return;
    }

    try {
      String mimeType = media.getString(mimeTypeIndex);
      boolean isVideo = mimeType != null && mimeType.startsWith("video");
      if(isVideo){
        Uri photoUri = Uri.parse("file://" + media.getString(dataIndex));
        @Nullable AssetFileDescriptor photoDescriptor = null;
        try {
          photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
        } catch (FileNotFoundException e) {
          FLog.e(ReactConstants.TAG, "Could not open asset file " + photoUri.toString(), e);
        }

        if (photoDescriptor != null) {
          MediaMetadataRetriever retriever = new MediaMetadataRetriever();
          try {
            retriever.setDataSource(photoDescriptor.getFileDescriptor());
          } catch (RuntimeException e) {
            // Do nothing. We can't handle this, and this is usually a system problem
          }
          try {
            String videoGeoTag = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
            if (videoGeoTag!=null){
              String filtered = videoGeoTag.replaceAll("/","");
              WritableMap location = new WritableNativeMap();
              location.putDouble("latitude", Double.parseDouble(filtered.split("[+]|[-]")[1]));
              location.putDouble("longitude", Double.parseDouble(filtered.split("[+]|[-]")[2]));
              node.putMap("location", location);
            }
          } catch (NumberFormatException e) {
            FLog.e(ReactConstants.TAG,"Number format exception occurred while trying to fetch video metadata for "+ photoUri.toString(),e);
          }
          try {
            retriever.release();
          } catch (Exception e) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
            // Do nothing. We can't handle this, and this is usually a system problem
          }
        }
        if (photoDescriptor != null) {
          try {
            photoDescriptor.close();
          } catch (IOException e) {
            // Do nothing. We can't handle this, and this is usually a system problem
          }
        }
      }
      else{
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
      }
    } catch (IOException e) {
      FLog.e(ReactConstants.TAG, "Could not read the metadata", e);
    }
  }

  /**
   * Delete a set of images.
   *
   * @param uris    array of file:// URIs of the images to delete
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

  @ReactMethod
  public void getPhotoByInternalID(String internalID, ReadableMap options, Promise promise) {
    promise.reject("CameraRoll:getPhotoByInternalID", "getPhotoByInternalID is not supported on Android");
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
      String[] projection = {MediaStore.Images.Media._ID};

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

  @ReactMethod
  public void getPhotoThumbnail(String internalID, ReadableMap options, Promise promise) {
    promise.reject("CameraRoll:getPhotoThumbnail", "getPhotoThumbnail is not supported on Android");
  }
}
