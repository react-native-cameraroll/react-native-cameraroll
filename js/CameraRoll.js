/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow
 * @format
 */
'use strict';
import {Platform} from 'react-native';
import RNCCameraRoll from './nativeInterface';

const invariant = require('fbjs/lib/invariant');

const GROUP_TYPES_OPTIONS = {
  Album: 'Album',
  All: 'All', // default
  Event: 'Event',
  Faces: 'Faces',
  Library: 'Library',
  PhotoStream: 'PhotoStream',
  SavedPhotos: 'SavedPhotos',
};

const ASSET_TYPE_OPTIONS = {
  All: 'All',
  Videos: 'Videos',
  Photos: 'Photos',
};

export type GroupTypes = $Keys<typeof GROUP_TYPES_OPTIONS>;

/**
 * Shape of the param arg for the `getPhotosFast` function.
 */
export type GetPhotosFastParams = {
  /**
   * The number of photos wanted in reverse order of the photo application
   * (i.e. most recent first).
   */
  first: number,

  /**
   * Specifies which group types to filter the results to.
   */
  groupTypes?: GroupTypes,

  /**
   * Specifies filter on group names, like 'Recent Photos' or custom album
   * titles.
   */
  groupName?: string,

  /**
   * Specifies filter on asset type
   */
  assetType?: $Keys<typeof ASSET_TYPE_OPTIONS>,

  /**
   * Earliest time to get photos from. A timestamp in milliseconds. Exclusive.
   */
  fromTime?: number,

  /**
   * Latest time to get photos from. A timestamp in milliseconds. Inclusive.
   */
  toTime?: Number,
};

/**
 * Shape of the param arg for the `getPhotos` function. This has a few more
 * parameters than the `getPhotosFast` params, at the cost of some
 * performance on iOS.
 */
export type GetPhotosParams = GetPhotosFastParams & {
  /**
   * A cursor that matches `page_info { end_cursor }` returned from a previous
   * call to `getPhotos`
   */
  after?: string,

  /**
   * Filter by mimetype (e.g. image/jpeg).
   */
  mimeTypes?: Array<string>,
};

/**
 * Params for the native `getPhotos` function, as implemented in the
 * RNCCameraRoll module.
 */
export type GetPhotosNativeParams = GetPhotosParams & {
  /**
   * If provided, it's OK for the output to have empty filenames. This can
   * improve performance on iOS when used by `getPhotosFast`.
   */
  allowEmptyFilenames?: boolean,
};

export type PhotoIdentifier = {
  node: {
    type: string,
    group_name: string,
    image: {
      filename: string,
      uri: string,
      height: number,
      width: number,
      fileSize: number,
      isStored?: boolean,
      playableDuration: number,
    },
    timestamp: number,
    location?: {
      latitude?: number,
      longitude?: number,
      altitude?: number,
      heading?: number,
      speed?: number,
    },
  },
};

export type PhotoIdentifiersPage = {
  edges: Array<PhotoIdentifier>,
  page_info: {
    has_next_page: boolean,
    start_cursor?: string,
    end_cursor?: string,
  },
};
export type SaveToCameraRollOptions = {
  type?: 'photo' | 'video' | 'auto',
  album?: string,
};

export type GetAlbumsParams = {
  assetType?: $Keys<typeof ASSET_TYPE_OPTIONS>,
};

export type Album = {
  title: string,
  count: number,
};

// Based on https://github.com/facebook/flow/issues/2405#issuecomment-256339492
type Exact<T> = T & $Shape<T>;

/**
 * `CameraRoll` provides access to the local camera roll or photo library.
 *
 * See https://facebook.github.io/react-native/docs/cameraroll.html
 */
class CameraRoll {
  static GroupTypesOptions = GROUP_TYPES_OPTIONS;
  static AssetTypeOptions = ASSET_TYPE_OPTIONS;

  /**
   * `CameraRoll.saveImageWithTag()` is deprecated. Use `CameraRoll.saveToCameraRoll()` instead.
   */
  static saveImageWithTag(tag: string): Promise<string> {
    console.warn(
      '`CameraRoll.saveImageWithTag()` is deprecated. Use `CameraRoll.saveToCameraRoll()` instead.',
    );
    return this.saveToCameraRoll(tag, 'photo');
  }

  /**
   * On iOS: requests deletion of a set of photos from the camera roll.
   * On Android: Deletes a set of photos from the camera roll.
   *
   */
  static deletePhotos(photoUris: Array<string>) {
    return RNCCameraRoll.deletePhotos(photoUris);
  }

  /**
   * Saves the photo or video to the camera roll or photo library.
   *
   */
  static save(
    tag: string,
    options: SaveToCameraRollOptions = {},
  ): Promise<string> {
    let {type = 'auto', album = ''} = options;
    invariant(
      typeof tag === 'string',
      'CameraRoll.saveToCameraRoll must be a valid string.',
    );
    invariant(
      options.type === 'photo' ||
        options.type === 'video' ||
        options.type === 'auto' ||
        options.type === undefined,
      `The second argument to saveToCameraRoll must be 'photo' or 'video' or 'auto'. You passed ${type ||
        'unknown'}`,
    );
    if (type === 'auto') {
      if (['mov', 'mp4'].indexOf(tag.split('.').slice(-1)[0]) >= 0) {
        type = 'video';
      } else {
        type = 'photo';
      }
    }
    return RNCCameraRoll.saveToCameraRoll(tag, {type, album});
  }
  static saveToCameraRoll(
    tag: string,
    type?: 'photo' | 'video' | 'auto',
  ): Promise<string> {
    console.warn(
      'CameraRoll.saveToCameraRoll(tag, type) is deprecated.  Use the save function instead',
    );
    return CameraRoll.save(tag, {type});
  }
  static getAlbums(
    params?: GetAlbumsParams = {assetType: ASSET_TYPE_OPTIONS.All},
  ): Promise<Album[]> {
    return RNCCameraRoll.getAlbums(params);
  }

  static getParamsWithDefaults<T: GetPhotosFastParams>(params: T): T {
    const newParams = {...params};
    if (!newParams.assetType) {
      newParams.assetType = ASSET_TYPE_OPTIONS.All;
    }
    if (!newParams.groupTypes && Platform.OS !== 'android') {
      newParams.groupTypes = GROUP_TYPES_OPTIONS.All;
    }
    return newParams;
  }
  /**
   * Returns a Promise with photo identifier objects from the local camera
   * roll of the device matching shape defined by `getPhotosReturnChecker`.
   *
   * See https://facebook.github.io/react-native/docs/cameraroll.html#getphotos
   */
  static getPhotos(params: GetPhotosParams): Promise<PhotoIdentifiersPage> {
    params = CameraRoll.getParamsWithDefaults(params);
    if (arguments.length > 1) {
      console.warn(
        'CameraRoll.getPhotos(tag, success, error) is deprecated.  Use the returned Promise instead',
      );
      let successCallback = arguments[1];
      const errorCallback = arguments[2] || (() => {});
      RNCCameraRoll.getPhotos(params).then(successCallback, errorCallback);
    }
    return RNCCameraRoll.getPhotos(params);
  }

  /**
   * Returns a Promise with photo identifier objects from the local camera
   * roll of the device matching shape defined by `getPhotosReturnChecker`.
   *
   * This is the same as `getPhotos` on Android, but is much faster on iOS.
   * For 1000 photos, it can save 4.8 out of 5 seconds. It does this by
   * not using cursor and mimetype filters, and by omitting the filename in
   * the returned object.
   */
  static getPhotosFast(
    params: GetPhotosFastParams,
  ): Promise<PhotoIdentifiersPage> {
    params = CameraRoll.getParamsWithDefaults(params);
    const nativeParams: Exact<GetPhotosNativeParams> = {
      ...params,
      allowEmptyFilenames: true,
    };
    return RNCCameraRoll.getPhotos(nativeParams);
  }
}

module.exports = CameraRoll;
