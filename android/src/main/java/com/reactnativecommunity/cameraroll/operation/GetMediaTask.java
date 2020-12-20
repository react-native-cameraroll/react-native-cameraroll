package com.reactnativecommunity.cameraroll.operation;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.ReactConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public class GetMediaTask extends GuardedAsyncTask<Void, Void> {

    private static final String ERROR_UNABLE_TO_LOAD_PERMISSION = "E_UNABLE_TO_LOAD_PERMISSION";
    private static final String SELECTION_BUCKET = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";

    private static final String[] PROJECTION = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATA
    };


    private static final String INCLUDE_FILENAME = "filename";
    private static final String INCLUDE_FILE_SIZE = "fileSize";
    private static final String INCLUDE_LOCATION = "location";
    private static final String INCLUDE_IMAGE_SIZE = "imageSize";
    private static final String INCLUDE_PLAYABLE_DURATION = "playableDuration";

    private static final String ERROR_UNABLE_TO_LOAD = "E_UNABLE_TO_LOAD";
    private static final String ERROR_UNABLE_TO_FILTER = "E_UNABLE_TO_FILTER";

    private static final String ASSET_TYPE_PHOTOS = "Photos";
    private static final String ASSET_TYPE_VIDEOS = "Videos";
    private static final String ASSET_TYPE_ALL = "All";

    private final Context mContext;
    private final int mFirst;
    private final @Nullable
    String mAfter;
    private final @Nullable String mGroupName;
    private final @Nullable
    ReadableArray mMimeTypes;
    private final Promise mPromise;
    private final String mAssetType;
    private final long mFromTime;
    private final long mToTime;
    private final Set<String> mInclude;

    public GetMediaTask(
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
            selection.append(" AND " + MediaStore.Images.Media.MIME_TYPE + " IN (");
            for (int i = 0; i < mMimeTypes.size(); i++) {
                selection.append("?,");
                selectionArgs.add(mMimeTypes.getString(i));
            }
            selection.replace(selection.length() - 1, selection.length(), ")");
        }

        if (mFromTime > 0) {
            selection.append(" AND " + MediaStore.Images.Media.DATE_TAKEN + " > ?");
            selectionArgs.add(mFromTime + "");
        }
        if (mToTime > 0) {
            selection.append(" AND " + MediaStore.Images.Media.DATE_TAKEN + " <= ?");
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
                    MediaStore.Images.Media.DATE_ADDED + " DESC, " + MediaStore.Images.Media.DATE_MODIFIED + " DESC");
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
        int mimeTypeIndex = media.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
        int groupNameIndex = media.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        int dateTakenIndex = media.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
        int widthIndex = media.getColumnIndex(MediaStore.MediaColumns.WIDTH);
        int heightIndex = media.getColumnIndex(MediaStore.MediaColumns.HEIGHT);
        int sizeIndex = media.getColumnIndex(MediaStore.MediaColumns.SIZE);
        int dataIndex = media.getColumnIndex(MediaStore.MediaColumns.DATA);

        boolean includeLocation = include.contains(INCLUDE_LOCATION);
        boolean includeFilename = include.contains(INCLUDE_FILENAME);
        boolean includeFileSize = include.contains(INCLUDE_FILE_SIZE);
        boolean includeImageSize = include.contains(INCLUDE_IMAGE_SIZE);
        boolean includePlayableDuration = include.contains(INCLUDE_PLAYABLE_DURATION);

        for (int i = 0; i < limit && !media.isAfterLast(); i++) {
            WritableMap edge = new WritableNativeMap();
            WritableMap node = new WritableNativeMap();
            boolean imageInfoSuccess =
                    putImageInfo(resolver, media, node, widthIndex, heightIndex, sizeIndex, dataIndex,
                            mimeTypeIndex, includeFilename, includeFileSize, includeImageSize,
                            includePlayableDuration);
            if (imageInfoSuccess) {
                putBasicNodeInfo(media, node, mimeTypeIndex, groupNameIndex, dateTakenIndex);
                putLocationInfo(media, node, dataIndex, includeLocation);

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
            int mimeTypeIndex,
            boolean includeFilename,
            boolean includeFileSize,
            boolean includeImageSize,
            boolean includePlayableDuration) {
        WritableMap image = new WritableNativeMap();
        Uri photoUri = Uri.parse("file://" + media.getString(dataIndex));
        image.putString("uri", photoUri.toString());
        String mimeType = media.getString(mimeTypeIndex);

        boolean isVideo = mimeType != null && mimeType.startsWith("video");
        boolean putImageSizeSuccess = putImageSize(resolver, media, image, widthIndex, heightIndex,
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
                int timeInMillisec =
                        Integer.parseInt(
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                playableDuration = timeInMillisec / 1000;
            } catch (NumberFormatException e) {
                success = false;
                FLog.e(
                        ReactConstants.TAG,
                        "Number format exception occurred while trying to fetch video metadata for "
                                + photoUri.toString(),
                        e);
            }
            retriever.release();
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

        if (width <= 0 || height <= 0) {
            @Nullable AssetFileDescriptor photoDescriptor = null;
            try {
                photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r");
            } catch (FileNotFoundException e) {
                success = false;
                FLog.e(ReactConstants.TAG, "Could not open asset file " + photoUri.toString(), e);
            }

            if (photoDescriptor != null) {
                if (isVideo) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(photoDescriptor.getFileDescriptor());
                    } catch (RuntimeException e) {
                        // Do nothing. We can't handle this, and this is usually a system problem
                    }
                    try {
                        width =
                                Integer.parseInt(
                                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                        height =
                                Integer.parseInt(
                                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    } catch (NumberFormatException e) {
                        success = false;
                        FLog.e(
                                ReactConstants.TAG,
                                "Number format exception occurred while trying to fetch video metadata for "
                                        + photoUri.toString(),
                                e);
                    }
                    retriever.release();
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    // Set inJustDecodeBounds to true so we don't actually load the Bitmap, but only get its
                    // dimensions instead.
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFileDescriptor(photoDescriptor.getFileDescriptor(), null, options);
                    width = options.outWidth;
                    height = options.outHeight;
                }

                try {
                    photoDescriptor.close();
                } catch (IOException e) {
                    // Do nothing. We can't handle this, and this is usually a system problem
                }
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
            boolean includeLocation) {
        node.putNull("location");

        if (!includeLocation) {
            return;
        }

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
}