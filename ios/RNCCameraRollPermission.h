//
//  CameraRollPermissionModule.h
//  RNCCameraRoll
//
//  Created by sakhi idris on 16/08/2022.
//  Copyright © 2022 Facebook. All rights reserved.
//
#import <AVFoundation/AVFoundation.h>
#ifdef RCT_NEW_ARCH_ENABLED
#import <rncameraroll/rncameraroll.h>
#else
#import <React/RCTBridge.h>
#endif
#import <React/RCTEventEmitter.h>
#import <Photos/Photos.h>
#import "RNCPermissionHelper.h"


@interface RNCCameraRollPermission : RCTEventEmitter

#ifdef RCT_NEW_ARCH_ENABLED
                                   <NativeCameraRollPermissionModuleSpec
#else
                                   <RCTBridgeModule
#endif
, PHPhotoLibraryChangeObserver>
@end
