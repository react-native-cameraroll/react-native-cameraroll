import {
  // @ts-ignore - remove this comment when RN in the repo & example app is upgraded
  TurboModuleRegistry,
  // @ts-ignore - remove this comment when RN in the repo & example app is upgraded
  TurboModule,
} from 'react-native';
import type {Int32} from 'react-native/Libraries/Types/CodegenTypes';

type CameraRollAuthorizationStatus =
  | 'granted'
  | 'limited'
  | 'denied'
  | 'unavailable'
  | 'blocked'
  | 'not-determined';

export interface Spec extends TurboModule {
  checkPermission(content: string): Promise<CameraRollAuthorizationStatus>;
  requestReadWritePermission(): Promise<CameraRollAuthorizationStatus>;
  requestAddOnlyPermission(): Promise<CameraRollAuthorizationStatus>;
  refreshPhotoSelection(): Promise<boolean>;
  addListener(eventName: string): void;
  removeListeners(count: Int32): void;
}

// we call get here since on Android this module does not exist and it would throw
export default TurboModuleRegistry.get<Spec>('RNCCameraRollPermission')!;
