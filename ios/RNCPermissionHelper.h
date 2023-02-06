#import <React/RCTUtils.h>
#import <React/RCTBridge.h>

typedef enum {
  RNPermissionStatusNotDetermined = 0,
  RNPermissionStatusRestricted = 1,
  RNPermissionStatusDenied = 2,
  RNPermissionStatusAuthorized = 3,
  RNPermissionStatusLimited = 4,
} RNPermissionStatus;

@interface RNCPermissionHelper : NSObject

+ (void)checkCameraRollPermission:(NSString *) accessLevel
                         resolver:(void (^ _Nonnull)(RNPermissionStatus))resolve
                         rejecter:(void (^ _Nonnull)(NSString *code, NSString *message))reject;

+ (void)requestCameraRollReadWritePermission:(void (^ _Nonnull)(RNPermissionStatus))resolve
                                    rejecter:(void (^ _Nonnull)(NSString *code, NSString *message))reject;

+ (void)requestCameraRollAddOnlyPermission:(void (^ _Nonnull)(RNPermissionStatus))resolve
                                  rejecter:(void (^ _Nonnull)(NSString *code, NSString *message))reject;

+ (void)refreshLimitedPhotoselection:(RCTPromiseResolveBlock _Nonnull)resolve
                            rejecter:(RCTPromiseRejectBlock _Nonnull)reject;
@end
