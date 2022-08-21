//
//  CameraRollPermissionModule.h
//  RNCCameraRoll
//
//  Created by sakhi idris on 16/08/2022.
//  Copyright © 2022 Facebook. All rights reserved.
//
#import <AVFoundation/AVFoundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <Photos/Photos.h>

typedef enum {
  RNPermissionStatusNotDetermined = 0,
  RNPermissionStatusRestricted = 1,
  RNPermissionStatusDenied = 2,
  RNPermissionStatusAuthorized = 3,
  RNPermissionStatusLimited = 4,
} RNPermissionStatus;

@interface RNCCameraRollPermissionModule : RCTEventEmitter <RCTBridgeModule, PHPhotoLibraryChangeObserver>

@end
