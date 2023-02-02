import {
    // @ts-ignore - remove this comment when RN in the repo & example app is upgraded
    TurboModuleRegistry,
    // @ts-ignore - remove this comment when RN in the repo & example app is upgraded
    TurboModule,
  } from 'react-native';
  
export interface Spec extends TurboModule {
    checkPermission(content: string): Promise<string>;
    requestReadWritePermission(): Promise<string>;
    requestAddOnlyPermission(): Promise<string>;
    refreshPhotoSelection(): Promise<string>;
}
  
// we call get here since on Android this module does not exist and it would throw
export default TurboModuleRegistry.get<Spec>('RNCCameraRollPermission');
