/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import <Photos/Photos.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <rncameraroll/rncameraroll.h>
#else
#import <React/RCTBridge.h>
#endif
#import <React/RCTConvert.h>

@interface RCTConvert (PHFetchOptions)

+ (PHFetchOptions *)PHFetchOptionsFromMediaType:(NSString *)mediaType
                                       fromTime:(NSUInteger)fromTime
                                         toTime:(NSUInteger)toTime;

@end


@interface RNCCameraRoll : NSObject
#ifdef RCT_NEW_ARCH_ENABLED
                                   <NativeCameraRollModuleSpec>
#else
                                   <RCTBridgeModule>
#endif

@end
