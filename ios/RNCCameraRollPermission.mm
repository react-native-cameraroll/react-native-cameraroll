//
//  CameraRollPermissionModule.m
//  RNCCameraRoll
//
//  Created by sakhi idris on 16/08/2022.
//  Copyright © 2022 Facebook. All rights reserved.
//
#import "RNCCameraRollPermission.h"
#import "RNCPermissionHelper.h"
#import <React/RCTUtils.h>
#import <React/RCTConvert.h>

@implementation RNCCameraRollPermission

{
  bool hasListeners;
}

// Will be called when this module's first listener is added.
-(void)startObserving {
  hasListeners = YES;
  [[PHPhotoLibrary sharedPhotoLibrary] registerChangeObserver:self];
}

// Will be called when this module's last listener is removed, or on dealloc.
-(void)stopObserving {
  hasListeners = NO;
  [[PHPhotoLibrary sharedPhotoLibrary] unregisterChangeObserver:self];
}

RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"onLibrarySelectionChange"];
}

- (NSString *)stringForStatus:(RNPermissionStatus)status {
  switch (status) {
    case RNPermissionStatusRestricted:
      return @"unavailable";
    case RNPermissionStatusNotDetermined:
      return @"not-determined";
    case RNPermissionStatusDenied:
      return @"denied";
    case RNPermissionStatusLimited:
      return @"limited";
    case RNPermissionStatusAuthorized:
      return @"granted";
  }
}

- (void)photoLibraryDidChange:(PHChange *)changeInstance
{
  if (hasListeners && changeInstance != nil) {
    [self sendEventWithName:@"onLibrarySelectionChange" body:@"Changes occured"];
  }
}

RCT_EXPORT_METHOD(checkPermission:
                  (NSString *) accessLevel
                  resolve: (RCTPromiseResolveBlock)resolve
                  reject: (RCTPromiseRejectBlock)reject) {

  [RNCPermissionHelper checkCameraRollPermission:accessLevel resolver:^(RNPermissionStatus status) {
    resolve([self stringForStatus:status]);
  } rejecter:^(NSString *code, NSString *message) {
    reject(code, message, nil);
  }];
}


RCT_EXPORT_METHOD(requestReadWritePermission:
                  (RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

  [RNCPermissionHelper requestCameraRollReadWritePermission:^(RNPermissionStatus status) {
    resolve([self stringForStatus:status]);
  } rejecter:^(NSString *code, NSString *message) {
    reject(code, message, nil);
  }];
}

RCT_EXPORT_METHOD(requestAddOnlyPermission:
                  (RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

  [RNCPermissionHelper requestCameraRollAddOnlyPermission:^(RNPermissionStatus status) {
    resolve([self stringForStatus:status]);
  } rejecter:^(NSString *code, NSString *message) {
    reject(code, message, nil);
  }];
}

RCT_EXPORT_METHOD(refreshPhotoSelection:
                 (RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject) {
  [RNCPermissionHelper refreshLimitedPhotoselection:resolve rejecter:reject];
}

#if RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeCameraRollPermissionModuleSpecJSI>(params);
}
#endif

@end

