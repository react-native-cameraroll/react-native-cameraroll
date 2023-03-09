/* eslint-disable @typescript-eslint/ban-types */
// we use Object type because methods on the native side use NSDictionary and ReadableMap
// and we want to stay compatible with those
import {TurboModuleRegistry, TurboModule} from 'react-native';

type Album = {
  title: string;
  count: number;
};

type PhotoIdentifier = {
  node: {
    type: string;
    group_name: string;
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
}

export default TurboModuleRegistry.getEnforcing<Spec>('RNCCameraRoll');
