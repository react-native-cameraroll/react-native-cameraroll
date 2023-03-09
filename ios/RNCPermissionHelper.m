#import <React/RCTUtils.h>
#import "RNCPermissionHelper.h"

@import Photos;
@import PhotosUI;

@implementation RNCPermissionHelper

#pragma mark - Access Levels
static NSString * const ADD_ONLY = @"addOnly";
static NSString * const READ_WRITE = @"readWrite";

+ (void)checkCameraRollPermission:(NSString *) accessLevel
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

+ (void)requestCameraRollReadWritePermission:(void (^ _Nonnull)(RNPermissionStatus))resolve
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

+ (void)requestCameraRollAddOnlyPermission:(void (^ _Nonnull)(RNPermissionStatus))resolve
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


+ (void)refreshLimitedPhotoselection:(RCTPromiseResolveBlock _Nonnull)resolve
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

@end
