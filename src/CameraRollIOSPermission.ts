import {NativeEventEmitter, Platform} from 'react-native';
import CameraRollPermissionModule from './NativeCameraRollPermissionModule';

/** Defines ios permission access levels for gallery */
export type AccessLevel = 'addOnly' | 'readWrite';

export type CameraRollAuthorizationStatus =
  | 'granted'
  | 'limited'
  | 'denied'
  | 'unavailable'
  | 'blocked'
  | 'not-determined';

const isIOS = Platform.OS === 'ios';
// eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
if (isIOS && CameraRollPermissionModule == null) {
  console.error(
    "photoLibraryPermissionModule: Native Module 'photoLibraryPermissionModule' was null! Did you run pod install?",
  );
}
export const cameraRollEventEmitter = new NativeEventEmitter(
  isIOS ? CameraRollPermissionModule : undefined,
);

export const iosReadGalleryPermission = (
  accessLevel: AccessLevel,
): Promise<CameraRollAuthorizationStatus> => {
  if (!isIOS) throw new Error('this module is available only for ios');

  return CameraRollPermissionModule.checkPermission(accessLevel);
};

export const iosRequestReadWriteGalleryPermission =
  (): Promise<CameraRollAuthorizationStatus> => {
    if (!isIOS) throw new Error('this module is available only for ios');

    return CameraRollPermissionModule.requestReadWritePermission();
  };

export const iosRequestAddOnlyGalleryPermission =
  (): Promise<CameraRollAuthorizationStatus> => {
    if (!isIOS) throw new Error('this module is available only for ios');

    return CameraRollPermissionModule.requestAddOnlyPermission();
  };

export const iosRefreshGallerySelection = (): Promise<boolean> => {
  if (!isIOS) throw new Error('this module is available only for ios');

  return CameraRollPermissionModule.refreshPhotoSelection();
};
