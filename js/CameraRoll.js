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
 * Shape of the param arg for the `getPhotos` function.
 */
export type GetPhotosParams = {
  /**
   * The number of photos wanted in reverse order of the photo application
   * (i.e. most recent first).
   */
  first: number,

  /**
   * A cursor that matches `page_info { end_cursor }` returned from a previous
   * call to `getPhotos`
   */
  after?: string,

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
   * Filter by mimetype (e.g. image/jpeg).
   */
  mimeTypes?: Array<string>,
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

  static deletePhotos(photos: Array<string>) {
    return RNCCameraRoll.deletePhotos(photos);
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
    return CameraRoll.save(tag, {type});
  }
  /**
   * Returns a Promise with photo identifier objects from the local camera
   * roll of the device matching shape defined by `getPhotosReturnChecker`.
   *
   * See https://facebook.github.io/react-native/docs/cameraroll.html#getphotos
   */
  static getPhotos(params: GetPhotosParams): Promise<PhotoIdentifiersPage> {
    if (!params.assetType) {
      params.assetType = ASSET_TYPE_OPTIONS.All;
    }
    if (!params.groupTypes && Platform.OS !== 'android') {
      params.groupTypes = GROUP_TYPES_OPTIONS.All;
    }
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
}

module.exports = CameraRoll;
