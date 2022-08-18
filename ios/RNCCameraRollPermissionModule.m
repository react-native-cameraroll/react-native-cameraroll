//
//  CameraRollPermissionModule.m
//  RNCCameraRoll
//
//  Created by sakhi idris on 16/08/2022.
//  Copyright © 2022 Facebook. All rights reserved.
//
#import "RNCCameraRollPermissionModule.h"
#import <React/RCTUtils.h>
#import <React/RCTConvert.h>

@import Photos;
@import PhotosUI;

@implementation RNCCameraRollPermissionModule

{
  bool hasListeners;
}

#pragma mark - Access Levels
static NSString * const ADD_ONLY = @"addOnly";
static NSString * const READ_WRITE = @"readWrite";

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



- (void)checkCameraRollPermission:(NSString *) accessLevel
                         resolver:(void (^ _Nonnull)(RNPermissionStatus))resolve
                         rejecter:(void (^ _Nonnull)(NSString *code, NSString *message))reject {
  PHAuthorizationStatus status;

  if (@available(iOS 14.0, *)) {
    PHAccessLevel requestedAccessLevel;
    if ([accessLevel isEqualToString: ADD_ONLY]) {
      requestedAccessLevel = PHAccessLevelAddOnly;
    } else if ([accessLevel isEqualToString: READ_WRITE]) {
      requestedAccessLevel = PHAccessLevelReadWrite;
    } else {
      return reject(@"incorrect_access_level", @"The requested access level does not exist");
    }
    status = [PHPhotoLibrary authorizationStatusForAccessLevel:requestedAccessLevel];
  } else {
    status = [PHPhotoLibrary authorizationStatus];
  }

  switch (status) {
    case PHAuthorizationStatusNotDetermined:
      return resolve(RNPermissionStatusNotDetermined);
    case PHAuthorizationStatusRestricted:
      return resolve(RNPermissionStatusRestricted);
    case PHAuthorizationStatusDenied:
      return resolve(RNPermissionStatusDenied);
    case PHAuthorizationStatusLimited:
      return resolve(RNPermissionStatusLimited);
    case PHAuthorizationStatusAuthorized:
      return resolve(RNPermissionStatusAuthorized);
  }

}

- (void)requestCameraRollReadWritePermission:(void (^ _Nonnull)(RNPermissionStatus))resolve
                                    rejecter:(void (^ _Nonnull)(NSString *code, NSString *message))reject {
  if (@available(iOS 14.0, *)) {
    [PHPhotoLibrary requestAuthorizationForAccessLevel:PHAccessLevelReadWrite handler:^(__unused PHAuthorizationStatus status) {
      [self checkCameraRollPermission: READ_WRITE resolver: resolve rejecter:reject];
    }];
  } else {
    [PHPhotoLibrary requestAuthorization:^(__unused PHAuthorizationStatus status) {
      [self checkCameraRollPermission: READ_WRITE resolver: resolve rejecter:reject];
    }];
  }
}

- (void)requestCameraRollAddOnlyPermission:(void (^ _Nonnull)(RNPermissionStatus))resolve
                                    rejecter:(void (^ _Nonnull)(NSString *code, NSString *message))reject {
  if (@available(iOS 14.0, *)) {
    [PHPhotoLibrary requestAuthorizationForAccessLevel:PHAccessLevelAddOnly handler:^(__unused PHAuthorizationStatus status) {
      [self checkCameraRollPermission: ADD_ONLY resolver: resolve rejecter:reject];
    }];
  } else {
    [PHPhotoLibrary requestAuthorization:^(__unused PHAuthorizationStatus status) {
      [self checkCameraRollPermission: ADD_ONLY resolver: resolve rejecter:reject];
    }];
  }
}


- (void)refreshLimitedPhotoselection:(RCTPromiseResolveBlock _Nonnull)resolve
                                         rejecter:(RCTPromiseRejectBlock _Nonnull)reject {
  if (@available(iOS 14, *)) {
    if ([PHPhotoLibrary authorizationStatusForAccessLevel:PHAccessLevelReadWrite] != PHAuthorizationStatusLimited) {
      return reject(@"cannot_open_limited_picker", @"Photo library permission isn't limited", nil);
    }

    UIViewController *presentedViewController = RCTPresentedViewController();
    [[PHPhotoLibrary sharedPhotoLibrary] presentLimitedLibraryPickerFromViewController:presentedViewController];

    resolve(@(true));
  } else {
    reject(@"cannot_open_limited_picker", @"Available on iOS 14 or higher", nil);
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

  [self checkCameraRollPermission:accessLevel resolver:^(RNPermissionStatus status) {
    resolve([self stringForStatus:status]);
  } rejecter:^(NSString *code, NSString *message) {
    reject(code, message, nil);
  }];
}


RCT_EXPORT_METHOD(requestReadWritePermission:
                  (RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

  [self requestCameraRollReadWritePermission:^(RNPermissionStatus status) {
    resolve([self stringForStatus:status]);
  } rejecter:^(NSString *code, NSString *message) {
    reject(code, message, nil);
  }];
}

RCT_EXPORT_METHOD(requestAddOnlyPermission:
                  (RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

  [self requestCameraRollAddOnlyPermission:^(RNPermissionStatus status) {
    resolve([self stringForStatus:status]);
  } rejecter:^(NSString *code, NSString *message) {
    reject(code, message, nil);
  }];
}


RCT_REMAP_METHOD(refreshPhotoSelection,
                 refreshLimitedPhotoselectionWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
  [self refreshLimitedPhotoselection:resolve rejecter:reject];
}

@end

