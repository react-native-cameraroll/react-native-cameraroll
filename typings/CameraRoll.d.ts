/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

declare namespace CameraRoll {
  type GroupType =
    | 'Album'
    | 'All'
    | 'Event'
    | 'Faces'
    | 'Library'
    | 'PhotoStream'
    | 'SavedPhotos';

  type AssetType = 'All' | 'Videos' | 'Photos';

  type Include =
    /** Ensures the filename is included. Has a large performance hit on iOS */
    | 'filename'
    /** Ensures the fileSize is included. Has a large performance hit on iOS */
    | 'fileSize'
    /** Ensures the location is included. Has a medium performance hit on Android */
    | 'location';

  /**
   * Shape of the param arg for the `getPhotosFast` function.
   */
  interface GetPhotosFastParams {
    /**
     * The number of photos wanted in reverse order of the photo application
     * (i.e. most recent first).
     */
    first: number;

    /**
     * A cursor that matches `page_info { end_cursor }` returned from a previous
     * call to `getPhotos`
     */
    after?: string;

    /**
     * Specifies which group types to filter the results to.
     */
    groupTypes?: GroupType;

    /**
     * Specifies filter on group names, like 'Recent Photos' or custom album
     * titles.
     */
    groupName?: string;

    /**
     * Specifies filter on asset type
     */
    assetType?: AssetType;

    /**
     * Earliest time to get photos from. A timestamp in milliseconds. Exclusive.
     */
    fromTime?: number;

    /**
     * Latest time to get photos from. A timestamp in milliseconds. Inclusive.
     */
    toTime?: number;

    /**
     * Filter by mimetype (e.g. image/jpeg).
     */
    mimeTypes?: Array<string>;

    /**
     * Specific fields in the output that we want to include, even though they
     * might have some performance impact.
     */
    include?: Include[];
  }

  /**
   * Params for the native `getPhotos` function, as implemented in the
   * RNCCameraRoll module.
   */
  interface GetPhotosNativeParams extends GetPhotosParams {
    /**
     * If provided, it's OK for the output to have empty filenames. This can
     * improve performance on iOS when used by `getPhotosFast`.
     */
    allowEmptyFilenames?: boolean;
  }

  interface PhotoIdentifier {
    node: {
      type: string;
      group_name: string;
      image: {
        filename: string;
        uri: string;
        height: number;
        width: number;
        fileSize: number;
        isStored?: boolean;
        playableDuration: number;
      };
      timestamp: number;
      location?: {
        latitude?: number;
        longitude?: number;
        altitude?: number;
        heading?: number;
        speed?: number;
      };
    };
  }

  interface PhotoIdentifiersPage {
    edges: Array<PhotoIdentifier>;
    page_info: {
      has_next_page: boolean;
      start_cursor?: string;
      end_cursor?: string;
    };
  }

  interface GetAlbumsParams {
    assetType?: AssetType;
  }

  interface Album {
    title: string;
    count: number;
  }

  type SaveToCameraRollOptions = {
    type?: 'photo' | 'video' | 'auto';
    album?: string;
  };

  /**
   * `CameraRoll.saveImageWithTag()` is deprecated. Use `CameraRoll.saveToCameraRoll()` instead.
   */
  function saveImageWithTag(tag: string): Promise<string>;

  /**
   * Delete a photo from the camera roll or media library. photoUris is an array of photo uri's.
   */
  function deletePhotos(photoUris: Array<string>): Promise<boolean>;

  /**
   * Saves the photo or video to the camera roll or photo library.
   */
  function saveToCameraRoll(
    tag: string,
    type?: 'photo' | 'video',
  ): Promise<string>;

  /**
   * Saves the photo or video to the camera roll or photo library.
   */
  function save(
    tag: string,
    options?: SaveToCameraRollOptions,
  ): Promise<string>;

  /**
   * Returns a Promise with photo identifier objects from the local camera
   * roll of the device matching shape defined by `getPhotosReturnChecker`.
   */
  function getPhotos(params: GetPhotosParams): Promise<PhotoIdentifiersPage>;

  function getAlbums(params: GetAlbumsParams): Promise<Album[]>;
}

export = CameraRoll;
