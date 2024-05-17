/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#pragma once

#if RCT_NEW_ARCH_ENABLED

#import <Foundation/Foundation.h>
#import <React/RCTURLRequestHandler.h>

@class PHPhotoLibrary;

// Uses the new CodeGen'd `modulesConformingToProtocol` feature.
// Supports fetching data from ph-upload:// assets to upload them (e.g. via fetch(..))
@interface RNCPHAssetUploader : NSObject <RCTURLRequestHandler>
@end

#endif
