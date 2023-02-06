/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import "RNCAssetsLibraryRequestHandler.h"

#import <stdatomic.h>
#import <dlfcn.h>
#import <objc/runtime.h>

#import <Photos/Photos.h>
#import <MobileCoreServices/MobileCoreServices.h>

#import <React/RCTBridge.h>
#import <React/RCTNetworking.h>
#import <React/RCTUtils.h>

@implementation RNCAssetsLibraryRequestHandler

NSString *const PHUploadScheme = @"ph-upload";

RCT_EXPORT_MODULE()

#pragma mark - RNCURLRequestHandler

- (BOOL)canHandleRequest:(NSURLRequest *)request
{
  if (![PHAsset class]) {
    return NO;
  }

  return [request.URL.scheme caseInsensitiveCompare:@"assets-library"] == NSOrderedSame
    || [request.URL.scheme caseInsensitiveCompare:@"ph"] == NSOrderedSame
    || [request.URL.scheme caseInsensitiveCompare:PHUploadScheme] == NSOrderedSame;
}

- (id)sendRequest:(NSURLRequest *)request
     withDelegate:(id<RCTURLRequestDelegate>)delegate
{
  __block atomic_bool cancelled = ATOMIC_VAR_INIT(NO);
  void (^cancellationBlock)(void) = ^{
    atomic_store(&cancelled, YES);
  };

  NSURL *requestURL = request.URL;
  BOOL isPHUpload = [requestURL.scheme caseInsensitiveCompare:PHUploadScheme] == NSOrderedSame;
  if (isPHUpload) {
    requestURL = [NSURL URLWithString:[@"ph" stringByAppendingString:[requestURL.absoluteString substringFromIndex:PHUploadScheme.length]]];
  }
  
  if (!requestURL) {
    NSString *const msg = [NSString stringWithFormat:@"Cannot send request without URL"];
    [delegate URLRequest:cancellationBlock didCompleteWithError:RCTErrorWithMessage(msg)];
    return cancellationBlock;
  }
  
  PHFetchResult<PHAsset *> *fetchResult;
 
  if ([requestURL.scheme caseInsensitiveCompare:@"ph"] == NSOrderedSame) {
    // Fetch assets using PHAsset localIdentifier (recommended)
    NSString *const localIdentifier = [requestURL.absoluteString substringFromIndex:@"ph://".length];
    fetchResult = [PHAsset fetchAssetsWithLocalIdentifiers:@[localIdentifier] options:nil];
  } else if ([requestURL.scheme caseInsensitiveCompare:@"assets-library"] == NSOrderedSame) {
    // This is the older, deprecated way of fetching assets from assets-library
    // using the "assets-library://" protocol
    fetchResult = [PHAsset fetchAssetsWithALAssetURLs:@[requestURL] options:nil];
  } else {
    NSString *const msg = [NSString stringWithFormat:@"Cannot send request with unknown protocol: %@", requestURL];
    [delegate URLRequest:cancellationBlock didCompleteWithError:RCTErrorWithMessage(msg)];
    return cancellationBlock;
  }
  
  if (![fetchResult firstObject]) {
    NSString *errorMessage = [NSString stringWithFormat:@"Failed to load asset"
                              " at URL %@ with no error message.", requestURL];
    NSError *error = RCTErrorWithMessage(errorMessage);
    [delegate URLRequest:cancellationBlock didCompleteWithError:error];
    return cancellationBlock;
  }
  
  if (atomic_load(&cancelled)) {
    return cancellationBlock;
  }

  PHAsset *const _Nonnull asset = [fetchResult firstObject];

  // When we're uploading a video, provide the full data but in any other case,
  // provide only the thumbnail of the video.
  if (asset.mediaType == PHAssetMediaTypeVideo && isPHUpload) {
    PHVideoRequestOptions *const requestOptions = [PHVideoRequestOptions new];
    requestOptions.networkAccessAllowed = YES;
    [[PHImageManager defaultManager] requestAVAssetForVideo:asset options:requestOptions resultHandler:^(AVAsset * _Nullable avAsset, AVAudioMix * _Nullable audioMix, NSDictionary * _Nullable info) {
      NSError *error = [info objectForKey:PHImageErrorKey];
      if (error) {
        [delegate URLRequest:cancellationBlock didCompleteWithError:error];
        return;
      }

      if (![avAsset isKindOfClass:[AVURLAsset class]]) {
        error = [NSError errorWithDomain:RCTErrorDomain code:0 userInfo:
        @{
          NSLocalizedDescriptionKey: @"Unable to load AVURLAsset",
          }];
        [delegate URLRequest:cancellationBlock didCompleteWithError:error];
        return;
      }

      NSData *data = [NSData dataWithContentsOfURL:((AVURLAsset *)avAsset).URL
                                           options:(NSDataReadingOptions)0
                                             error:&error];
      if (data) {
        NSURLResponse *const response = [[NSURLResponse alloc] initWithURL:request.URL MIMEType:nil expectedContentLength:data.length textEncodingName:nil];
        [delegate URLRequest:cancellationBlock didReceiveResponse:response];
        [delegate URLRequest:cancellationBlock didReceiveData:data];
      }
      [delegate URLRequest:cancellationBlock didCompleteWithError:error];
    }];
  } else {
    // By default, allow downloading images from iCloud
    PHImageRequestOptions *const requestOptions = [PHImageRequestOptions new];
    requestOptions.networkAccessAllowed = YES;

    [[PHImageManager defaultManager] requestImageDataForAsset:asset
                                                      options:requestOptions
                                                resultHandler:^(NSData * _Nullable imageData,
                                                                NSString * _Nullable dataUTI,
                                                                UIImageOrientation orientation,
                                                                NSDictionary * _Nullable info) {
      NSError *const error = [info objectForKey:PHImageErrorKey];
      if (error) {
        [delegate URLRequest:cancellationBlock didCompleteWithError:error];
        return;
      }

      NSInteger const length = [imageData length];
      CFStringRef const dataUTIStringRef = (__bridge CFStringRef _Nonnull)(dataUTI);
      CFStringRef const mimeType = UTTypeCopyPreferredTagWithClass(dataUTIStringRef, kUTTagClassMIMEType);

      NSURLResponse *const response = [[NSURLResponse alloc] initWithURL:request.URL
                                                                MIMEType:(__bridge NSString *)(mimeType)
                                                   expectedContentLength:length
                                                        textEncodingName:nil];
      if (mimeType) CFRelease(mimeType);

      [delegate URLRequest:cancellationBlock didReceiveResponse:response];

      [delegate URLRequest:cancellationBlock didReceiveData:imageData];
      [delegate URLRequest:cancellationBlock didCompleteWithError:nil];
    }];
  }
  
  return cancellationBlock;
}

- (void)cancelRequest:(id)requestToken
{
  ((void (^)(void))requestToken)();
}

//- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
//    (const facebook::react::ObjCTurboModule::InitParams &)params
//{
//  return nullptr;
//}

@end
