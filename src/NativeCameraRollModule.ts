/* eslint-disable @typescript-eslint/ban-types */
// we use Object type because methods on the native side use NSDictionary and ReadableMap
// and we want to stay compatible with those
import {TurboModuleRegistry, TurboModule} from 'react-native';
import type { PhotoThumbnail } from './CameraRoll';

export type AlbumType = 'All' | 'Album' | 'SmartAlbum';

export type AlbumSubType =
  | 'AlbumRegular'
  | 'AlbumSyncedEvent'
  | 'AlbumSyncedFaces'
  | 'AlbumSyncedAlbum'
  | 'AlbumImported'
  | 'AlbumMyPhotoStream'
  | 'AlbumCloudShared'
  | 'Unknown';

type Album = {
  title: string;
  count: number;
  type: AlbumType;
  subtype?: AlbumSubType;
};

type SubTypes =
  | 'PhotoPanorama'
  | 'PhotoHDR'
  | 'PhotoScreenshot'
  | 'PhotoLive'
  | 'PhotoDepthEffect'
  | 'VideoStreamed'
  | 'VideoHighFrameRate'
  | 'VideoTimelapse';

type PhotoIdentifier = {
  node: {
    id: string;
    type: string;
    subTypes: SubTypes;
    group_name: string[];
    image: {
      filename: string | null;
      filepath: string | null;
      extension: string | null;
      uri: string;
      height: number;
      width: number;
      fileSize: number | null;
      playableDuration: number;
      orientation: number | null;
    };
    timestamp: number;
    modificationTimestamp: number;
    location: {
      latitude?: number;
      longitude?: number;
      altitude?: number;
      heading?: number;
      speed?: number;
    } | null;
  };
};

type PhotoIdentifiersPage = {
  edges: Array<PhotoIdentifier>;
  page_info: {
    has_next_page: boolean;
    start_cursor?: string;
    end_cursor?: string;
  };
  limited?: boolean;
};

export interface Spec extends TurboModule {
  saveToCameraRoll(uri: string, options: Object): Promise<string>;
  getPhotos(params: Object): Promise<PhotoIdentifiersPage>;
  getAlbums(params: Object): Promise<Album[]>;
  deletePhotos(photoUris: Array<string>): Promise<void>;
  getPhotoByInternalID(
    internalID: string,
    options: Object,
  ): Promise<PhotoIdentifier>;
  getPhotoThumbnail(
    internalID: string,
    options: Object
  ): Promise<PhotoThumbnail>
}

export default TurboModuleRegistry.getEnforcing<Spec>('RNCCameraRoll');
