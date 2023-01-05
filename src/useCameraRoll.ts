import {useState} from 'react';
import type {
  GetPhotosParams,
  PhotoIdentifiersPage,
  SaveToCameraRollOptions,
} from './CameraRoll';
import {CameraRoll} from './CameraRoll';

const initialState: PhotoIdentifiersPage = {
  edges: [],
  page_info: {
    end_cursor: '',
    has_next_page: false,
    start_cursor: '',
  },
};

const defaultConfig: GetPhotosParams = {
  first: 20,
  groupTypes: 'All',
};

type UseCameraRollResult = [
  PhotoIdentifiersPage,
  (config?: GetPhotosParams) => Promise<void>,
  (tag: string, options?: SaveToCameraRollOptions) => Promise<void>,
];

export function useCameraRoll(): UseCameraRollResult {
  const [photos, setPhotos] = useState<PhotoIdentifiersPage>(initialState);

  async function getPhotos(
    config: GetPhotosParams = defaultConfig,
  ): Promise<void> {
    try {
      const result = await CameraRoll.getPhotos(config);
      setPhotos(result);
    } catch (error) {
      if (__DEV__) console.log('[useCameraRoll] Error getting photos: ', error);
    }
  }

  async function save(
    ...args: Parameters<typeof CameraRoll.save>
  ): Promise<void> {
    try {
      await CameraRoll.save(...args);
    } catch (error) {
      if (__DEV__)
        console.log('[useCameraRoll] Error saving to camera roll: ', error);
    }
  }

  return [photos, getPhotos, save];
}
