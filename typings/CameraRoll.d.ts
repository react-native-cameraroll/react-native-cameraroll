/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
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

  interface GetPhotosParams {
    first: number;
    after?: string;
    groupTypes?: GroupType;
    groupName?: string;
    assetType?: AssetType;
    mimeTypes?: Array<string>;
    includeExifTimestamp?: boolean;
  }

  interface PhotoIdentifier {
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
    };
  }

  interface PhotoIdentifiersPage {
    edges: Array<PhotoIdentifier>;
    page_info: {
      has_next_page: boolean,
      start_cursor?: string,
      end_cursor?: string,
    };
  }

  type SaveToCameraRollOptions = {
    type?: 'photo' | 'video' | 'auto',
    album?: string,
  };

    /**
     * `CameraRoll.saveImageWithTag()` is deprecated. Use `CameraRoll.saveToCameraRoll()` instead.
     */
    function saveImageWithTag(tag: string): Promise<string>;

    /**
     * Delete a photo from the camera roll or media library. photos is an array of photo uri's.
     */
    function deletePhotos(photos: Array<string>): void;
    // deletePhotos: (photos: Array<string>) => void;

    /**
     * Saves the photo or video to the camera roll or photo library.
     */
    function saveToCameraRoll(tag: string, type?: 'photo' | 'video'): Promise<string>;

    /**
     * Saves the photo or video to the camera roll or photo library.
     */
    function save(tag: string, options?: SaveToCameraRollOptions): Promise<string> 

    /**
     * Returns a Promise with photo identifier objects from the local camera
     * roll of the device matching shape defined by `getPhotosReturnChecker`.
     */
    function getPhotos(params: GetPhotosParams): Promise<PhotoIdentifiersPage>;
}

export = CameraRoll;
