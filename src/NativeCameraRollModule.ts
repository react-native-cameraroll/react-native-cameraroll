import {
    // @ts-ignore - remove this comment when RN in the repo & example app is upgraded
    TurboModuleRegistry,
    // @ts-ignore - remove this comment when RN in the repo & example app is upgraded
    TurboModule,
} from 'react-native';
  
type Album = {
    title: string;
    count: number;
};

export type PhotoIdentifier = {
    node: {
      type: string;
      group_name: string;
      image: {
        filename?: string;
        filepath?: string;
        extension?: string;
        uri: string;
        height: number;
        width: number;
        fileSize?: number;
        playableDuration: number;
        orientation?: number;
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

export interface Spec extends TurboModule {
    saveToCameraRoll(uri: string, params: Object): Promise<string>;
    getPhotos(params: Object): Promise<PhotoIdentifiersPage>; 
    getAlbums(params: Object): Promise<Album[]>;
    deletePhotos(photoUris: Array<string>): Promise<void>;
}
  
export default TurboModuleRegistry.getEnforcing<Spec>('RNCCameraRoll');
