/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
import {Platform} from 'react-native';
import RNCCameraRoll from './nativeInterface';

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

export type GroupTypes =
  | 'Album'
  | 'All'
  | 'Event'
  | 'Faces'
  | 'Library'
  | 'PhotoStream'
  | 'SavedPhotos';

export type Include =
  | 'filename'
  | 'fileSize'
  | 'location'
  | 'imageSize'
  | 'playableDuration';

export type AssetType = 'All' | 'Videos' | 'Photos';

/**
 * Shape of the param arg for the `getPhotos` function.
 */
export type GetPhotosParams = {
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
  groupTypes?: GroupTypes;

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
};

export type PhotoIdentifier = {
  node: {
    type: string;
    group_name: string;
    image: {
      filename: string | null;
      uri: string;
      height: number;
      width: number;
      fileSize: number | null;
      playableDuration: number;
    };
    timestamp: number;
    location: {
      latitude?: number;
      longitude?: number;
      altitude?: number;
      heading?: number;
      speed?: number;
    } | null;
  };
};

export type PhotoConvertionOptions = {
  convertHeicImages: boolean;
};

export type PhotoIdentifiersPage = {
  edges: Array<PhotoIdentifier>;
  page_info: {
    has_next_page: boolean;
    start_cursor?: string;
    end_cursor?: string;
  };
  limited?: boolean;
};

export type SaveToCameraRollOptions = {
  type?: 'photo' | 'video' | 'auto';
  album?: string;
};

export type GetAlbumsParams = {
  assetType?: AssetType;
};

export type Album = {
  title: string;
  count: number;
};

/**
 * `CameraRoll` provides access to the local camera roll or photo library.
 *
 * See https://facebook.github.io/react-native/docs/cameraroll.html
 */
export class CameraRoll {
  static GroupTypesOptions = GROUP_TYPES_OPTIONS;
  static AssetTypeOptions = ASSET_TYPE_OPTIONS;

  /**
   * On iOS: requests deletion of a set of photos from the camera roll.
   * On Android: Deletes a set of photos from the camera roll.
   *
   */
  static deletePhotos(photoUris: Array<string>): void {
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
    let {type = 'auto'} = options;
    const {album = ''} = options;
    if (tag === '') throw new Error('tag must be a valid string');

    if (type === 'auto') {
      const fileExtension = tag.split('.').slice(-1)[0] ?? '';
      if (['mov', 'mp4'].indexOf(fileExtension) >= 0) type = 'video';
      else type = 'photo';
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
    params: GetAlbumsParams = {assetType: 'All'},
  ): Promise<Album[]> {
    return RNCCameraRoll.getAlbums(params);
  }

  static getParamsWithDefaults(params: GetPhotosParams): GetPhotosParams {
    const newParams = {...params};
    if (newParams.assetType === undefined) newParams.assetType = 'All';

    if (newParams.groupTypes === undefined && Platform.OS !== 'android')
      newParams.groupTypes = 'All';

    return newParams;
  }

  /**
   * Returns a Promise with photo identifier objects from the local camera
   * roll of the device matching shape defined by `getPhotosReturnChecker`.
   *
   * See https://facebook.github.io/react-native/docs/cameraroll.html#getphotos
   */
  static getPhotos(params: GetPhotosParams): Promise<PhotoIdentifiersPage> {
    params = this.getParamsWithDefaults(params);
    return RNCCameraRoll.getPhotos(params);
  }

  /**
   * Returns a Promise with photo internal path.
   * if conversion is requested from HEIC then temporary file is created.
   *
   * @param internalID - PH photo internal ID.
   * @param convertHeicImages - whether to convert or not heic images to JPEG.
   * @returns Promise<PhotoIdentifier>
   */
  static iosGetImageDataById(
    internalID: string,
    convertHeicImages = false,
  ): Promise<PhotoIdentifier> {
    const conversionOption: PhotoConvertionOptions = {
      convertHeicImages: convertHeicImages,
    };
    return RNCCameraRoll.getPhotoByInternalID(internalID, conversionOption);
  }
}
