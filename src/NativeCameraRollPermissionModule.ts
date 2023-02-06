import {
    TurboModuleRegistry,
    TurboModule,
  } from 'react-native';
import type { Int32 } from 'react-native/Libraries/Types/CodegenTypes';
  
export interface Spec extends TurboModule {
    checkPermission(content: string): Promise<string>;
    requestReadWritePermission(): Promise<string>;
    requestAddOnlyPermission(): Promise<string>;
    refreshPhotoSelection(): Promise<string>;
    addListener(eventName: string): void;
    removeListeners(count: Int32): void;
}
  
// we call get here since on Android this module does not exist and it would throw
export default TurboModuleRegistry.get<Spec>('RNCCameraRollPermission')!;
