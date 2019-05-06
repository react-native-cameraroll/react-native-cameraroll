/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

export type GroupType =
  'Album' |
  'All' |
  'Event' |
  'Faces' |
  'Library' |
  'PhotoStream' |
  'SavedPhotos';


export type AssetType =
  'All' |
  'Videos' |
  'Photos';

export interface GetPhotosParams {
  first: number,
  after?: string,
  groupTypes?: GroupType,
  groupName?: string,
  assetType?: AssetType,
  mimeTypes?: Array<string>,
}

export interface PhotoIdentifier {
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
}

export interface PhotoIdentifiersPage {
  edges: Array<PhotoIdentifier>,
  page_info: {
    has_next_page: boolean,
    start_cursor?: string,
    end_cursor?: string,
  },
}
export type SaveToCameraRollOptions = {
  type?: 'photo' | 'video' | 'auto',
  album?: string,
};

export interface CameraRollStatic {
  /**
   * `CameraRoll.saveImageWithTag()` is deprecated. Use `CameraRoll.saveToCameraRoll()` instead.
   */
  saveImageWithTag: (tag: string) => Promise<string>;

  /**
   * Delete a photo from the camera roll or media library. photos is an array of photo uri's.
   */
  deletePhotos: (photos: Array<string>) => void;

  /**
   * Saves the photo or video to the camera roll or photo library.
   */
  saveToCameraRoll(tag: string, options?: SaveToCameraRollOptions): Promise<string> 

  /**
   * Returns a Promise with photo identifier objects from the local camera
   * roll of the device matching shape defined by `getPhotosReturnChecker`.
   */
  getPhotos: (params: GetPhotosParams) => Promise<PhotoIdentifiersPage>;
}

declare let CameraRoll: CameraRollStatic;

export default CameraRoll;
