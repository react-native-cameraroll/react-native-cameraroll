/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#pragma once

#if RCT_NEW_ARCH_ENABLED

#import <Foundation/Foundation.h>
#import <React/RCTImageURLLoader.h>

@class PHPhotoLibrary;

// Uses the new CodeGen'd `modulesConformingToProtocol` feature.
// Supports loading Images from ph:// and asset-library:// URLs.
@interface RNCPHAssetLoader : NSObject <RCTImageURLLoader>
@end

#endif
