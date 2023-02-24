/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.reactnativecommunity.cameraroll

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import com.facebook.common.logging.FLog
import com.facebook.react.bridge.*
import com.facebook.react.common.ReactConstants
import com.facebook.react.module.annotations.ReactModule
import com.reactnativecommunity.cameraroll.Utils.getExtension
import com.reactnativecommunity.cameraroll.Utils.getMimeType
import java.io.*

/**
 * [NativeModule] that allows JS to interact with the photos and videos on the device (i.e.
 * [MediaStore.Images]).
 */
@ReactModule(name = CameraRollModule.NAME)
class CameraRollModule(reactContext: ReactApplicationContext?) :
    ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String {
        return NAME
    }

    /**
     * Save an image to the gallery (i.e. [MediaStore.Images]). This copies the original file
     * from wherever it may be to the external storage pictures directory, so that it can be scanned
     * by the MediaScanner.
     *
     * @param uri     the file:// URI of the image to save
     * @param promise to be resolved or rejected
     */
    @ReactMethod
    fun saveToCameraRoll(uri: String?, options: ReadableMap, promise: Promise) {
        SaveToCameraRoll(reactApplicationContext, Uri.parse(uri), options, promise)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private class SaveToCameraRoll(
        context: ReactContext,
        uri: Uri,
        options: ReadableMap,
        promise: Promise
    ) : GuardedAsyncTask<Void?, Void?>(context) {
        private val mContext: Context
        private val mUri: Uri
        private val mPromise: Promise
        private val mOptions: ReadableMap

        init {
            mContext = context
            mUri = uri
            mPromise = promise
            mOptions = options
        }

        override fun doInBackgroundGuarded(vararg params: Void?) {
            val source = File(mUri.path)
            var input: FileInputStream? = null
            var output: OutputStream? = null
            val mimeType = getMimeType(mUri.toString())
            val isVideo = mimeType != null && mimeType.contains("video")
            try {
                val album = mOptions.getString("album")
                val isAlbumPresent = !TextUtils.isEmpty(album)

                // Android Q and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val mediaDetails = ContentValues()
                    if (isAlbumPresent) {
                        val relativePath = Environment.DIRECTORY_DCIM + File.separator + album
                        mediaDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    }
                    mediaDetails.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    mediaDetails.put(MediaStore.Images.Media.DISPLAY_NAME, source.name)
                    mediaDetails.put(MediaStore.Images.Media.IS_PENDING, 1)
                    val resolver = mContext.contentResolver
                    val mediaContentUri = if (isVideo) resolver.insert(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        mediaDetails
                    ) else resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        mediaDetails
                    )
                    output = resolver.openOutputStream(mediaContentUri!!)
                    input = FileInputStream(source)
                    FileUtils.copy(input, output!!)
                    mediaDetails.clear()
                    mediaDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(mediaContentUri, mediaDetails, null, null)
                    mPromise.resolve(mediaContentUri.toString())
                } else {
                    val environment: File
                    // Media is not saved into an album when using Environment.DIRECTORY_DCIM.
                    environment = if (isAlbumPresent) {
                        if ("video" == mOptions.getString("type")) {
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                        } else {
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        }
                    } else {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    }
                    val exportDir: File
                    if (isAlbumPresent) {
                        exportDir = File(environment, album)
                        if (!exportDir.exists() && !exportDir.mkdirs()) {
                            mPromise.reject(
                                ERROR_UNABLE_TO_LOAD,
                                "Album Directory not created. Did you request WRITE_EXTERNAL_STORAGE?"
                            )
                            return
                        }
                    } else {
                        exportDir = environment
                    }
                    if (!exportDir.isDirectory) {
                        mPromise.reject(
                            ERROR_UNABLE_TO_LOAD,
                            "External media storage directory not available"
                        )
                        return
                    }
                    var dest = File(exportDir, source.name)
                    var n = 0
                    val fullSourceName = source.name
                    val sourceName: String
                    val sourceExt: String
                    if (fullSourceName.indexOf('.') >= 0) {
                        sourceName = fullSourceName.substring(0, fullSourceName.lastIndexOf('.'))
                        sourceExt = fullSourceName.substring(fullSourceName.lastIndexOf('.'))
                    } else {
                        sourceName = fullSourceName
                        sourceExt = ""
                    }
                    while (!dest.createNewFile()) {
                        dest = File(exportDir, sourceName + "_" + n++ + sourceExt)
                    }
                    input = FileInputStream(source)
                    output = FileOutputStream(dest)
                    output.channel
                        .transferFrom(input.channel, 0, input.channel.size())
                    input.close()
                    output.close()
                    MediaScannerConnection.scanFile(
                        mContext, arrayOf(dest.absolutePath),
                        null
                    ) { path: String?, uri: Uri? ->
                        if (uri != null) {
                            mPromise.resolve(uri.toString())
                        } else {
                            mPromise.reject(ERROR_UNABLE_TO_SAVE, "Could not add image to gallery")
                        }
                    }
                }
            } catch (e: IOException) {
                mPromise.reject(e)
            } finally {
                if (input != null) {
                    try {
                        input.close()
                    } catch (e: IOException) {
                        FLog.e(ReactConstants.TAG, "Could not close input channel", e)
                    }
                }
                if (output != null) {
                    try {
                        output.close()
                    } catch (e: IOException) {
                        FLog.e(ReactConstants.TAG, "Could not close output channel", e)
                    }
                }
            }
        }
    }

    /**
     * Get photos from [MediaStore.Images], most recent first.
     *
     * @param params  a map containing the following keys:
     *
     *  * first (mandatory): a number representing the number of photos to fetch
     *  *
     * after (optional): a cursor that matches page_info[end_cursor] returned by a
     * previous call to [.getPhotos]
     *
     *  * groupName (optional): an album name
     *  *
     * mimeType (optional): restrict returned images to a specific mimetype (e.g.
     * image/jpeg)
     *
     *  *
     * assetType (optional): chooses between either photos or videos from the camera roll.
     * Valid values are "Photos" or "Videos". Defaults to photos.
     *
     *
     * @param promise the Promise to be resolved when the photos are loaded; for a format of the
     * parameters passed to this callback, see `getPhotosReturnChecker` in CameraRoll.js
     */
    @ReactMethod
    fun getPhotos(params: ReadableMap, promise: Promise) {
        val first = params.getInt("first")
        val after = if (params.hasKey("after")) params.getString("after") else null
        val groupName = if (params.hasKey("groupName")) params.getString("groupName") else null
        val assetType =
            if (params.hasKey("assetType")) params.getString("assetType") else ASSET_TYPE_PHOTOS
        val fromTime = if (params.hasKey("fromTime")) params.getDouble("fromTime").toLong() else 0
        val toTime = if (params.hasKey("toTime")) params.getDouble("toTime").toLong() else 0
        val mimeTypes = if (params.hasKey("mimeTypes")) params.getArray("mimeTypes") else null
        val include = if (params.hasKey("include")) params.getArray("include") else null
        GetMediaTask(
            reactApplicationContext,
            first,
            after,
            groupName,
            mimeTypes,
            assetType,
            fromTime,
            toTime,
            include,
            promise
        )
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private class GetMediaTask(
        context: ReactContext,
        first: Int,
        after: String?,
        groupName: String?,
        mimeTypes: ReadableArray?,
        assetType: String?,
        fromTime: Long,
        toTime: Long,
        include: ReadableArray?,
        promise: Promise
    ) : GuardedAsyncTask<Void?, Void?>(context) {
        private val mContext: Context
        private val mFirst: Int
        private val mAfter: String?
        private val mGroupName: String?
        private val mMimeTypes: ReadableArray?
        private val mPromise: Promise
        private val mAssetType: String?
        private val mFromTime: Long
        private val mToTime: Long
        private val mInclude: Set<String>

        init {
            mContext = context
            mFirst = first
            mAfter = after
            mGroupName = groupName
            mMimeTypes = mimeTypes
            mPromise = promise
            mAssetType = assetType
            mFromTime = fromTime
            mToTime = toTime
            mInclude = createSetFromIncludeArray(include)
        }

        override fun doInBackgroundGuarded(vararg params: Void?) {
            val selection = StringBuilder("1")
            val selectionArgs: MutableList<String?> = ArrayList()
            if (!TextUtils.isEmpty(mGroupName)) {
                selection.append(" AND " + SELECTION_BUCKET)
                selectionArgs.add(mGroupName)
            }
            if (mAssetType == ASSET_TYPE_PHOTOS) {
                selection.append(
                    " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                            + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                )
            } else if (mAssetType == ASSET_TYPE_VIDEOS) {
                selection.append(
                    " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                            + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                )
            } else if (mAssetType == ASSET_TYPE_ALL) {
                selection.append(
                    " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " IN ("
                            + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ","
                            + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + ")"
                )
            } else {
                mPromise.reject(
                    ERROR_UNABLE_TO_FILTER,
                    "Invalid filter option: '" + mAssetType + "'. Expected one of '"
                            + ASSET_TYPE_PHOTOS + "', '" + ASSET_TYPE_VIDEOS + "' or '" + ASSET_TYPE_ALL + "'."
                )
                return
            }
            if (mMimeTypes != null && mMimeTypes.size() > 0) {
                selection.append(" AND " + MediaStore.Images.Media.MIME_TYPE + " IN (")
                for (i in 0 until mMimeTypes.size()) {
                    selection.append("?,")
                    selectionArgs.add(mMimeTypes.getString(i))
                }
                selection.replace(selection.length - 1, selection.length, ")")
            }
            if (mFromTime > 0) {
                val addedDate = mFromTime / 1000
                selection.append(
                    " AND (" + MediaStore.Images.Media.DATE_TAKEN + " > ? OR ( " + MediaStore.Images.Media.DATE_TAKEN
                            + " IS NULL AND " + MediaStore.Images.Media.DATE_ADDED + "> ? ))"
                )
                selectionArgs.add(mFromTime.toString() + "")
                selectionArgs.add(addedDate.toString() + "")
            }
            if (mToTime > 0) {
                val addedDate = mToTime / 1000
                selection.append(
                    " AND (" + MediaStore.Images.Media.DATE_TAKEN + " <= ? OR ( " + MediaStore.Images.Media.DATE_TAKEN
                            + " IS NULL AND " + MediaStore.Images.Media.DATE_ADDED + " <= ? ))"
                )
                selectionArgs.add(mToTime.toString() + "")
                selectionArgs.add(addedDate.toString() + "")
            }
            val response: WritableMap = WritableNativeMap()
            val resolver = mContext.contentResolver
            try {
                val media: Cursor?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bundle = Bundle()
                    bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.toString())
                    bundle.putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        selectionArgs.toTypedArray()
                    )
                    bundle.putString(
                        ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                        MediaStore.Images.Media.DATE_ADDED + " DESC, " + MediaStore.Images.Media.DATE_MODIFIED + " DESC"
                    )
                    bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, mFirst + 1)
                    if (!TextUtils.isEmpty(mAfter)) {
                        bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, mAfter!!.toInt())
                    }
                    media = resolver.query(
                        MediaStore.Files.getContentUri("external"),
                        PROJECTION,
                        bundle,
                        null
                    )
                } else {
                    // set LIMIT to first + 1 so that we know how to populate page_info
                    var limit = "limit=" + (mFirst + 1)
                    if (!TextUtils.isEmpty(mAfter)) {
                        limit = "limit=" + mAfter + "," + (mFirst + 1)
                    }
                    media = resolver.query(
                        MediaStore.Files.getContentUri("external").buildUpon().encodedQuery(limit)
                            .build(),
                        PROJECTION,
                        selection.toString(),
                        selectionArgs.toTypedArray(),
                        MediaStore.Images.Media.DATE_ADDED + " DESC, " + MediaStore.Images.Media.DATE_MODIFIED + " DESC"
                    )
                }
                if (media == null) {
                    mPromise.reject(ERROR_UNABLE_TO_LOAD, "Could not get media")
                } else {
                    try {
                        putEdges(resolver, media, response, mFirst, mInclude)
                        putPageInfo(
                            media,
                            response,
                            mFirst,
                            if (!TextUtils.isEmpty(mAfter)) mAfter!!.toInt() else 0
                        )
                    } finally {
                        media.close()
                        mPromise.resolve(response)
                    }
                }
            } catch (e: SecurityException) {
                mPromise.reject(
                    ERROR_UNABLE_TO_LOAD_PERMISSION,
                    "Could not get media: need READ_EXTERNAL_STORAGE permission",
                    e
                )
            }
        }

        companion object {
            private fun createSetFromIncludeArray(includeArray: ReadableArray?): Set<String> {
                val includeSet: MutableSet<String> = HashSet()
                if (includeArray == null) {
                    return includeSet
                }
                for (i in 0 until includeArray.size()) {
                    val includeItem = includeArray.getString(i)
                    if (includeItem != null) {
                        includeSet.add(includeItem)
                    }
                }
                return includeSet
            }
        }
    }

    @ReactMethod
    fun getAlbums(params: ReadableMap, promise: Promise) {
        val assetType =
            if (params.hasKey("assetType")) params.getString("assetType") else ASSET_TYPE_ALL
        val selection = StringBuilder("1")
        val selectionArgs: List<String> = ArrayList()
        if (assetType == ASSET_TYPE_PHOTOS) {
            selection.append(
                " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            )
        } else if (assetType == ASSET_TYPE_VIDEOS) {
            selection.append(
                " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            )
        } else if (assetType == ASSET_TYPE_ALL) {
            selection.append(
                " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " IN ("
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ","
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + ")"
            )
        } else {
            promise.reject(
                ERROR_UNABLE_TO_FILTER,
                "Invalid filter option: '" + assetType + "'. Expected one of '"
                        + ASSET_TYPE_PHOTOS + "', '" + ASSET_TYPE_VIDEOS + "' or '" + ASSET_TYPE_ALL + "'."
            )
            return
        }
        val projection = arrayOf(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
        try {
            val media = reactApplicationContext.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection.toString(),
                selectionArgs.toTypedArray(),
                null
            )
            if (media == null) {
                promise.reject(ERROR_UNABLE_TO_LOAD, "Could not get media")
            } else {
                val response: WritableArray = WritableNativeArray()
                try {
                    if (media.moveToFirst()) {
                        val albums: MutableMap<String, Int> = HashMap()
                        do {
                            val column =
                                media.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
                            if (column < 0) {
                                throw IndexOutOfBoundsException()
                            }
                            val albumName = media.getString(column)
                            if (albumName != null) {
                                val albumCount = albums[albumName]
                                if (albumCount == null) {
                                    albums[albumName] = 1
                                } else {
                                    albums[albumName] = albumCount + 1
                                }
                            }
                        } while (media.moveToNext())
                        for ((key, value) in albums) {
                            val album: WritableMap = WritableNativeMap()
                            album.putString("title", key)
                            album.putInt("count", value)
                            response.pushMap(album)
                        }
                    }
                } finally {
                    media.close()
                    promise.resolve(response)
                }
            }
        } catch (e: Exception) {
            promise.reject(ERROR_UNABLE_TO_LOAD, "Could not get media", e)
        }
    }

    /**
     * Delete a set of images.
     *
     * @param uris    array of file:// URIs of the images to delete
     * @param promise to be resolved
     */
    @ReactMethod
    fun deletePhotos(uris: ReadableArray, promise: Promise) {
        if (uris.size() == 0) {
            promise.reject(ERROR_UNABLE_TO_DELETE, "Need at least one URI to delete")
        } else {
            DeletePhotos(reactApplicationContext, uris, promise)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private class DeletePhotos(context: ReactContext, uris: ReadableArray, promise: Promise) :
        GuardedAsyncTask<Void?, Void?>(context) {
        private val mContext: Context
        private val mUris: ReadableArray
        private val mPromise: Promise

        init {
            mContext = context
            mUris = uris
            mPromise = promise
        }

        override fun doInBackgroundGuarded(vararg params: Void?) {
            val resolver = mContext.contentResolver

            // Set up the projection (we only need the ID)
            val projection = arrayOf(MediaStore.Images.Media._ID)

            // Match on the file path
            var innerWhere = "?"
            for (i in 1 until mUris.size()) {
                innerWhere += ", ?"
            }
            val selection = MediaStore.Images.Media.DATA + " IN (" + innerWhere + ")"
            // Query for the ID of the media matching the file path
            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selectionArgs = arrayOfNulls<String>(mUris.size())
            for (i in 0 until mUris.size()) {
                val uri = Uri.parse(mUris.getString(i))
                selectionArgs[i] = uri.path
            }
            val cursor = resolver.query(queryUri, projection, selection, selectionArgs, null)
            var deletedCount = 0
            while (cursor!!.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val deleteUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                if (resolver.delete(deleteUri, null, null) == 1) {
                    deletedCount++
                }
            }
            cursor.close()
            if (deletedCount == mUris.size()) {
                mPromise.resolve(true)
            } else {
                mPromise.reject(
                    ERROR_UNABLE_TO_DELETE,
                    "Could not delete all media, only deleted $deletedCount photos."
                )
            }
        }
    }

    companion object {
        const val NAME = "RNCCameraRoll"
        private const val ERROR_UNABLE_TO_LOAD = "E_UNABLE_TO_LOAD"
        private const val ERROR_UNABLE_TO_LOAD_PERMISSION = "E_UNABLE_TO_LOAD_PERMISSION"
        private const val ERROR_UNABLE_TO_SAVE = "E_UNABLE_TO_SAVE"
        private const val ERROR_UNABLE_TO_DELETE = "E_UNABLE_TO_DELETE"
        private const val ERROR_UNABLE_TO_FILTER = "E_UNABLE_TO_FILTER"
        private const val ASSET_TYPE_PHOTOS = "Photos"
        private const val ASSET_TYPE_VIDEOS = "Videos"
        private const val ASSET_TYPE_ALL = "All"
        private const val INCLUDE_FILENAME = "filename"
        private const val INCLUDE_FILE_SIZE = "fileSize"
        private const val INCLUDE_FILE_EXTENSION = "fileExtension"
        private const val INCLUDE_LOCATION = "location"
        private const val INCLUDE_IMAGE_SIZE = "imageSize"
        private const val INCLUDE_PLAYABLE_DURATION = "playableDuration"
        private const val INCLUDE_ORIENTATION = "orientation"
        private val PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.ORIENTATION
        )
        private const val SELECTION_BUCKET = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?"
        private fun putPageInfo(media: Cursor, response: WritableMap, limit: Int, offset: Int) {
            val pageInfo: WritableMap = WritableNativeMap()
            pageInfo.putBoolean("has_next_page", limit < media.count)
            if (limit < media.count) {
                pageInfo.putString(
                    "end_cursor",
                    Integer.toString(offset + limit)
                )
            }
            response.putMap("page_info", pageInfo)
        }

        private fun putEdges(
            resolver: ContentResolver,
            media: Cursor,
            response: WritableMap,
            limit: Int,
            include: Set<String>
        ) {
            val edges: WritableArray = WritableNativeArray()
            media.moveToFirst()
            val mimeTypeIndex = media.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val groupNameIndex = media.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateTakenIndex = media.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedIndex = media.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val dateModifiedIndex = media.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthIndex = media.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIndex = media.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val sizeIndex = media.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dataIndex = media.getColumnIndex(MediaStore.MediaColumns.DATA)
            val orientationIndex = media.getColumnIndex(MediaStore.MediaColumns.ORIENTATION)
            val includeLocation = include.contains(INCLUDE_LOCATION)
            val includeFilename = include.contains(INCLUDE_FILENAME)
            val includeFileSize = include.contains(INCLUDE_FILE_SIZE)
            val includeFileExtension = include.contains(INCLUDE_FILE_EXTENSION)
            val includeImageSize = include.contains(INCLUDE_IMAGE_SIZE)
            val includePlayableDuration = include.contains(INCLUDE_PLAYABLE_DURATION)
            val includeOrientation = include.contains(INCLUDE_ORIENTATION)
            var i = 0
            while (i < limit && !media.isAfterLast) {
                val edge: WritableMap = WritableNativeMap()
                val node: WritableMap = WritableNativeMap()
                val imageInfoSuccess = putImageInfo(
                    resolver,
                    media,
                    node,
                    widthIndex,
                    heightIndex,
                    sizeIndex,
                    dataIndex,
                    orientationIndex,
                    mimeTypeIndex,
                    includeFilename,
                    includeFileSize,
                    includeFileExtension,
                    includeImageSize,
                    includePlayableDuration,
                    includeOrientation
                )
                if (imageInfoSuccess) {
                    putBasicNodeInfo(
                        media,
                        node,
                        mimeTypeIndex,
                        groupNameIndex,
                        dateTakenIndex,
                        dateAddedIndex,
                        dateModifiedIndex
                    )
                    putLocationInfo(
                        media,
                        node,
                        dataIndex,
                        includeLocation,
                        mimeTypeIndex,
                        resolver
                    )
                    edge.putMap("node", node)
                    edges.pushMap(edge)
                } else {
                    // we skipped an image because we couldn't get its details (e.g. width/height), so we
                    // decrement i in order to correctly reach the limit, if the cursor has enough rows
                    i--
                }
                media.moveToNext()
                i++
            }
            response.putArray("edges", edges)
        }

        private fun putBasicNodeInfo(
            media: Cursor,
            node: WritableMap,
            mimeTypeIndex: Int,
            groupNameIndex: Int,
            dateTakenIndex: Int,
            dateAddedIndex: Int,
            dateModifiedIndex: Int
        ) {
            node.putString("type", media.getString(mimeTypeIndex))
            node.putString("group_name", media.getString(groupNameIndex))
            var dateTaken = media.getLong(dateTakenIndex)
            if (dateTaken == 0L) {
                //date added is in seconds, date taken in milliseconds, thus the multiplication
                dateTaken = media.getLong(dateAddedIndex) * 1000
            }
            node.putDouble("timestamp", dateTaken / 1000.0)
            node.putDouble("modified", media.getLong(dateModifiedIndex).toDouble())
        }

        /**
         * @return Whether we successfully fetched all the information about the image that we were asked
         * to include
         */
        private fun putImageInfo(
            resolver: ContentResolver,
            media: Cursor,
            node: WritableMap,
            widthIndex: Int,
            heightIndex: Int,
            sizeIndex: Int,
            dataIndex: Int,
            orientationIndex: Int,
            mimeTypeIndex: Int,
            includeFilename: Boolean,
            includeFileSize: Boolean,
            includeFileExtension: Boolean,
            includeImageSize: Boolean,
            includePlayableDuration: Boolean,
            includeOrientation: Boolean
        ): Boolean {
            val image: WritableMap = WritableNativeMap()
            val photoUri = Uri.parse("file://" + media.getString(dataIndex))
            image.putString("uri", photoUri.toString())
            val mimeType = media.getString(mimeTypeIndex)
            val isVideo = mimeType != null && mimeType.startsWith("video")
            val putImageSizeSuccess = putImageSize(
                resolver, media, image, widthIndex, heightIndex, orientationIndex,
                photoUri, isVideo, includeImageSize
            )
            val putPlayableDurationSuccess = putPlayableDuration(
                resolver, image, photoUri, isVideo,
                includePlayableDuration
            )
            if (includeFilename) {
                val file = File(media.getString(dataIndex))
                val strFileName = file.name
                image.putString("filename", strFileName)
            } else {
                image.putNull("filename")
            }
            if (includeFileSize) {
                image.putDouble("fileSize", media.getLong(sizeIndex).toDouble())
            } else {
                image.putNull("fileSize")
            }
            if (includeFileExtension) {
                image.putString("extension", getExtension(mimeType))
            } else {
                image.putNull("extension")
            }
            if (includeOrientation) {
                if (media.isNull(orientationIndex)) {
                    image.putInt("orientation", media.getInt(orientationIndex))
                } else {
                    image.putInt("orientation", 0)
                }
            } else {
                image.putNull("orientation")
            }
            node.putMap("image", image)
            return putImageSizeSuccess && putPlayableDurationSuccess
        }

        /**
         * @return Whether we succeeded in fetching and putting the playableDuration
         */
        private fun putPlayableDuration(
            resolver: ContentResolver,
            image: WritableMap,
            photoUri: Uri,
            isVideo: Boolean,
            includePlayableDuration: Boolean
        ): Boolean {
            image.putNull("playableDuration")
            if (!includePlayableDuration || !isVideo) {
                return true
            }
            var success = true
            var playableDuration: Int? = null
            var photoDescriptor: AssetFileDescriptor? = null
            try {
                photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r")
            } catch (e: FileNotFoundException) {
                success = false
                FLog.e(ReactConstants.TAG, "Could not open asset file $photoUri", e)
            }
            if (photoDescriptor != null) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(photoDescriptor.fileDescriptor)
                } catch (e: RuntimeException) {
                    // Do nothing. We can't handle this, and this is usually a system problem
                }
                try {
                    val timeInMillisecond =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                            .toInt()
                    playableDuration = timeInMillisecond / 1000
                } catch (e: NumberFormatException) {
                    success = false
                    FLog.e(
                        ReactConstants.TAG,
                        "Number format exception occurred while trying to fetch video metadata for "
                                + photoUri.toString(),
                        e
                    )
                }
                try {
                    retriever.release()
                } catch (e: Exception) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
                    // Do nothing. We can't handle this, and this is usually a system problem
                }
            }
            if (photoDescriptor != null) {
                try {
                    photoDescriptor.close()
                } catch (e: IOException) {
                    // Do nothing. We can't handle this, and this is usually a system problem
                }
            }
            if (playableDuration != null) {
                image.putInt("playableDuration", playableDuration)
            }
            return success
        }

        private fun putImageSize(
            resolver: ContentResolver,
            media: Cursor,
            image: WritableMap,
            widthIndex: Int,
            heightIndex: Int,
            orientationIndex: Int,
            photoUri: Uri,
            isVideo: Boolean,
            includeImageSize: Boolean
        ): Boolean {
            image.putNull("width")
            image.putNull("height")
            if (!includeImageSize) {
                return true
            }
            var success = true
            var width = media.getInt(widthIndex)
            var height = media.getInt(heightIndex)

            /* If the columns don't contain the size information, read the media file */if (width <= 0 || height <= 0) {
                var mediaDescriptor: AssetFileDescriptor? = null
                try {
                    mediaDescriptor = resolver.openAssetFileDescriptor(photoUri, "r")
                } catch (e: FileNotFoundException) {
                    success = false
                    FLog.e(ReactConstants.TAG, "Could not open asset file $photoUri", e)
                }
                if (mediaDescriptor != null) {
                    if (isVideo) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(mediaDescriptor.fileDescriptor)
                        } catch (e: RuntimeException) {
                            // Do nothing. We can't handle this, and this is usually a system problem
                        }
                        try {
                            width =
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!
                                    .toInt()
                            height =
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!
                                    .toInt()
                        } catch (e: NumberFormatException) {
                            success = false
                            FLog.e(
                                ReactConstants.TAG,
                                "Number format exception occurred while trying to fetch video metadata for "
                                        + photoUri.toString(),
                                e
                            )
                        }
                        try {
                            retriever.release()
                        } catch (e: Exception) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
                            // Do nothing. We can't handle this, and this is usually a system problem
                        }
                    } else {
                        val options = BitmapFactory.Options()
                        // Set inJustDecodeBounds to true so we don't actually load the Bitmap, but only get its
                        // dimensions instead.
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFileDescriptor(
                            mediaDescriptor.fileDescriptor,
                            null,
                            options
                        )
                        width = options.outWidth
                        height = options.outHeight
                    }
                    try {
                        mediaDescriptor.close()
                    } catch (e: IOException) {
                        FLog.e(
                            ReactConstants.TAG, "Can't close media descriptor "
                                    + photoUri.toString(),
                            e
                        )
                    }
                }
            }
            if (!media.isNull(orientationIndex)) {
                val orientation = media.getInt(orientationIndex)
                if (orientation >= 0 && orientation % 180 != 0) {
                    val temp = width
                    width = height
                    height = temp
                }
            }
            image.putInt("width", width)
            image.putInt("height", height)
            return success
        }

        private fun putLocationInfo(
            media: Cursor,
            node: WritableMap,
            dataIndex: Int,
            includeLocation: Boolean,
            mimeTypeIndex: Int,
            resolver: ContentResolver
        ) {
            node.putNull("location")
            if (!includeLocation) {
                return
            }
            try {
                val mimeType = media.getString(mimeTypeIndex)
                val isVideo = mimeType != null && mimeType.startsWith("video")
                if (isVideo) {
                    val photoUri = Uri.parse("file://" + media.getString(dataIndex))
                    var photoDescriptor: AssetFileDescriptor? = null
                    try {
                        photoDescriptor = resolver.openAssetFileDescriptor(photoUri, "r")
                    } catch (e: FileNotFoundException) {
                        FLog.e(ReactConstants.TAG, "Could not open asset file $photoUri", e)
                    }
                    if (photoDescriptor != null) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(photoDescriptor.fileDescriptor)
                        } catch (e: RuntimeException) {
                            // Do nothing. We can't handle this, and this is usually a system problem
                        }
                        try {
                            val videoGeoTag =
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                            if (videoGeoTag != null) {
                                val filtered = videoGeoTag.replace("/".toRegex(), "")
                                val location: WritableMap = WritableNativeMap()
                                location.putDouble(
                                    "latitude",
                                    filtered.split("[+]|[-]".toRegex())
                                        .dropLastWhile { it.isEmpty() }
                                        .toTypedArray()[1].toDouble())
                                location.putDouble(
                                    "longitude",
                                    filtered.split("[+]|[-]".toRegex())
                                        .dropLastWhile { it.isEmpty() }
                                        .toTypedArray()[2].toDouble())
                                node.putMap("location", location)
                            }
                        } catch (e: NumberFormatException) {
                            FLog.e(
                                ReactConstants.TAG,
                                "Number format exception occurred while trying to fetch video metadata for $photoUri",
                                e
                            )
                        }
                        try {
                            retriever.release()
                        } catch (e: Exception) { // Use general Exception here, see: https://developer.android.com/reference/android/media/MediaMetadataRetriever#release()
                            // Do nothing. We can't handle this, and this is usually a system problem
                        }
                    }
                    if (photoDescriptor != null) {
                        try {
                            photoDescriptor.close()
                        } catch (e: IOException) {
                            // Do nothing. We can't handle this, and this is usually a system problem
                        }
                    }
                } else {
                    // location details are no longer indexed for privacy reasons using string Media.LATITUDE, Media.LONGITUDE
                    // we manually obtain location metadata using ExifInterface#getLatLong(float[]).
                    // ExifInterface is added in API level 5
                    val exif = ExifInterface(media.getString(dataIndex))
                    val imageCoordinates = FloatArray(2)
                    val hasCoordinates = exif.getLatLong(imageCoordinates)
                    if (hasCoordinates) {
                        val longitude = imageCoordinates[1].toDouble()
                        val latitude = imageCoordinates[0].toDouble()
                        val location: WritableMap = WritableNativeMap()
                        location.putDouble("longitude", longitude)
                        location.putDouble("latitude", latitude)
                        node.putMap("location", location)
                    }
                }
            } catch (e: IOException) {
                FLog.e(ReactConstants.TAG, "Could not read the metadata", e)
            }
        }
    }
}