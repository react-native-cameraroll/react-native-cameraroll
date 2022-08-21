/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import "RNCCameraRollManager.h"

#import <CoreLocation/CoreLocation.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <Photos/Photos.h>
#import <dlfcn.h>
#import <objc/runtime.h>
#import <MobileCoreServices/UTType.h>

#import <React/RCTBridge.h>
#import <React/RCTConvert.h>
#import <React/RCTLog.h>
#import <React/RCTUtils.h>

#import "RNCAssetsLibraryRequestHandler.h"

@implementation RCTConvert (PHAssetCollectionSubtype)

RCT_ENUM_CONVERTER(PHAssetCollectionSubtype, (@{
   @"album": @(PHAssetCollectionSubtypeAny),
   @"all": @(PHAssetCollectionSubtypeSmartAlbumUserLibrary),
   @"event": @(PHAssetCollectionSubtypeAlbumSyncedEvent),
   @"faces": @(PHAssetCollectionSubtypeAlbumSyncedFaces),
   @"library": @(PHAssetCollectionSubtypeSmartAlbumUserLibrary),
   @"photo-stream": @(PHAssetCollectionSubtypeAlbumMyPhotoStream), // incorrect, but legacy
   @"photostream": @(PHAssetCollectionSubtypeAlbumMyPhotoStream),
   @"saved-photos": @(PHAssetCollectionSubtypeAny), // incorrect, but legacy correspondence in PHAssetCollectionSubtype
   @"savedphotos": @(PHAssetCollectionSubtypeAny), // This was ALAssetsGroupSavedPhotos, seems to have no direct correspondence in PHAssetCollectionSubtype
}), PHAssetCollectionSubtypeAny, integerValue)


@end

@implementation RCTConvert (PHFetchOptions)

+ (PHFetchOptions *)PHFetchOptionsFromMediaType:(NSString *)mediaType
                                       fromTime:(NSUInteger)fromTime
                                         toTime:(NSUInteger)toTime
{
  // This is not exhaustive in terms of supported media type predicates; more can be added in the future
  NSString *const lowercase = [mediaType lowercaseString];
  NSMutableArray *format = [NSMutableArray new];
  NSMutableArray *arguments = [NSMutableArray new];

  if ([lowercase isEqualToString:@"photos"]) {
    [format addObject:@"mediaType = %d"];
    [arguments addObject:@(PHAssetMediaTypeImage)];
  } else if ([lowercase isEqualToString:@"videos"]) {
    [format addObject:@"mediaType = %d"];
    [arguments addObject:@(PHAssetMediaTypeVideo)];
  } else {
    if (![lowercase isEqualToString:@"all"]) {
      RCTLogError(@"Invalid filter option: '%@'. Expected one of 'photos',"
                  "'videos' or 'all'.", mediaType);
    }
  }

  if (fromTime > 0) {
    NSDate* fromDate = [NSDate dateWithTimeIntervalSince1970:fromTime/1000];
    [format addObject:@"creationDate > %@"];
    [arguments addObject:fromDate];
  }
  if (toTime > 0) {
    NSDate* toDate = [NSDate dateWithTimeIntervalSince1970:toTime/1000];
    [format addObject:@"creationDate <= %@"];
    [arguments addObject:toDate];
  }

  // This case includes the "all" mediatype
  PHFetchOptions *const options = [PHFetchOptions new];
  if ([format count] > 0) {
    options.predicate = [NSPredicate predicateWithFormat:[format componentsJoinedByString:@" AND "] argumentArray:arguments];
  }
  return options;
}

@end

@implementation RNCCameraRollManager

RCT_EXPORT_MODULE(RNCCameraRoll)

@synthesize bridge = _bridge;

static NSString *const kErrorUnableToSave = @"E_UNABLE_TO_SAVE";
static NSString *const kErrorUnableToLoad = @"E_UNABLE_TO_LOAD";

static NSString *const kErrorAuthRestricted = @"E_PHOTO_LIBRARY_AUTH_RESTRICTED";
static NSString *const kErrorAuthDenied = @"E_PHOTO_LIBRARY_AUTH_DENIED";

typedef void (^PhotosAuthorizedBlock)(bool isLimited);

static void requestPhotoLibraryAccess(RCTPromiseRejectBlock reject, PhotosAuthorizedBlock authorizedBlock, bool requestAddOnly) {
  PHAuthorizationStatus authStatus;
  if (@available(iOS 14, *)) {
      if (requestAddOnly) {
        authStatus = [PHPhotoLibrary authorizationStatusForAccessLevel:PHAccessLevelAddOnly];
      } else {
        authStatus = [PHPhotoLibrary authorizationStatusForAccessLevel:PHAccessLevelReadWrite];
      }
  } else {
    authStatus = [PHPhotoLibrary authorizationStatus];
  }
  if (authStatus == PHAuthorizationStatusRestricted) {
    reject(kErrorAuthRestricted, @"Access to photo library is restricted", nil);
  } else if (authStatus == PHAuthorizationStatusAuthorized) {
    authorizedBlock(false);
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunguarded-availability-new"
  } else if (authStatus == PHAuthorizationStatusLimited) {
#pragma clang diagnostic pop
    authorizedBlock(true);
  } else if (authStatus == PHAuthorizationStatusNotDetermined) {
      if (@available(iOS 14, *)) {
          if (requestAddOnly) {
              [PHPhotoLibrary requestAuthorizationForAccessLevel:PHAccessLevelAddOnly handler:^(PHAuthorizationStatus status) {
                  requestPhotoLibraryAccess(reject, authorizedBlock, requestAddOnly);
              }];
          } else {
              [PHPhotoLibrary requestAuthorizationForAccessLevel:PHAccessLevelReadWrite handler:^(PHAuthorizationStatus status) {
                  requestPhotoLibraryAccess(reject, authorizedBlock, requestAddOnly);
              }];
          }
      } else {
          [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
              requestPhotoLibraryAccess(reject, authorizedBlock, requestAddOnly);
          }];
      }
  } else {
    reject(kErrorAuthDenied, @"Access to photo library was denied", nil);
  }
}

RCT_EXPORT_METHOD(saveToCameraRoll:(NSURLRequest *)request
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  // We load images and videos differently.
  // Images have many custom loaders which can load images from ALAssetsLibrary URLs, PHPhotoLibrary
  // URLs, `data:` URIs, etc. Video URLs are passed directly through for now; it may be nice to support
  // more ways of loading videos in the future.
  __block NSURL *inputURI = nil;
  __block PHFetchResult *photosAsset;
  __block PHAssetCollection *collection;
  __block PHObjectPlaceholder *placeholder;

  void (^saveBlock)(void) = ^void() {
    // performChanges and the completionHandler are called on
    // arbitrary threads, not the main thread - this is safe
    // for now since all JS is queued and executed on a single thread.
    // We should reevaluate this if that assumption changes.

    [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
      PHAssetChangeRequest *assetRequest ;
      if ([options[@"type"] isEqualToString:@"video"]) {
        assetRequest = [PHAssetChangeRequest creationRequestForAssetFromVideoAtFileURL:inputURI];
      } else {
        NSData *data = [NSData dataWithContentsOfURL:inputURI];
        UIImage *image = [UIImage imageWithData:data];
        assetRequest = [PHAssetChangeRequest creationRequestForAssetFromImage:image];
      }
      placeholder = [assetRequest placeholderForCreatedAsset];
      if (![options[@"album"] isEqualToString:@""]) {
        photosAsset = [PHAsset fetchAssetsInAssetCollection:collection options:nil];
        PHAssetCollectionChangeRequest *albumChangeRequest = [PHAssetCollectionChangeRequest changeRequestForAssetCollection:collection assets:photosAsset];
        [albumChangeRequest addAssets:@[placeholder]];
      }
    } completionHandler:^(BOOL success, NSError *error) {
      if (success) {
        NSString *uri = [NSString stringWithFormat:@"ph://%@", [placeholder localIdentifier]];
        resolve(uri);
      } else {
        reject(kErrorUnableToSave, nil, error);
      }
    }];
  };
  void (^saveWithOptions)(void) = ^void() {
    if (![options[@"album"] isEqualToString:@""]) {

      PHFetchOptions *fetchOptions = [[PHFetchOptions alloc] init];
      fetchOptions.predicate = [NSPredicate predicateWithFormat:@"title = %@", options[@"album"] ];
      collection = [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeAlbum
                                                            subtype:PHAssetCollectionSubtypeAny
                                                            options:fetchOptions].firstObject;
      // Create the album
      if (!collection) {
        [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
          PHAssetCollectionChangeRequest *createAlbum = [PHAssetCollectionChangeRequest creationRequestForAssetCollectionWithTitle:options[@"album"]];
          placeholder = [createAlbum placeholderForCreatedAssetCollection];
        } completionHandler:^(BOOL success, NSError *error) {
          if (success) {
            PHFetchResult *collectionFetchResult = [PHAssetCollection fetchAssetCollectionsWithLocalIdentifiers:@[placeholder.localIdentifier]
                                                                                                        options:nil];
            collection = collectionFetchResult.firstObject;
            saveBlock();
          } else {
            reject(kErrorUnableToSave, nil, error);
          }
        }];
      } else {
        saveBlock();
      }
    } else {
      saveBlock();
    }
  };

  void (^loadBlock)(bool isLimited) = ^void(bool isLimited) {
    inputURI = request.URL;
    saveWithOptions();
  };

  requestPhotoLibraryAccess(reject, loadBlock, true);
}

RCT_EXPORT_METHOD(getAlbums:(NSDictionary *)params
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSString *const mediaType = [params objectForKey:@"assetType"] ? [RCTConvert NSString:params[@"assetType"]] : @"All";
  PHFetchOptions* options = [[PHFetchOptions alloc] init];
  PHFetchResult<PHAssetCollection *> *const assetCollectionFetchResult = [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeAlbum subtype:PHAssetCollectionSubtypeAny options:options];
  NSMutableArray * result = [NSMutableArray new];
  [assetCollectionFetchResult enumerateObjectsUsingBlock:^(PHAssetCollection * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
    PHFetchOptions *const assetFetchOptions = [RCTConvert PHFetchOptionsFromMediaType:mediaType fromTime:0 toTime:0];
    // Enumerate assets within the collection
    PHFetchResult<PHAsset *> *const assetsFetchResult = [PHAsset fetchAssetsInAssetCollection:obj options:assetFetchOptions];
    if (assetsFetchResult.count > 0) {
      [result addObject:@{
        @"title": [obj localizedTitle],
        @"count": @(assetsFetchResult.count)
      }];
    }
  }];
  resolve(result);
}

static void RCTResolvePromise(RCTPromiseResolveBlock resolve,
                              NSArray<NSDictionary<NSString *, id> *> *assets,
                              BOOL hasNextPage,
                              bool isLimited)
{
  if (!assets.count) {
    resolve(@{
      @"edges": assets,
      @"page_info": @{
        @"has_next_page": @NO,
      },
      @"limited": @(isLimited)
    });
    return;
  }
  resolve(@{
    @"edges": assets,
    @"page_info": @{
      @"start_cursor": assets[0][@"node"][@"image"][@"uri"],
      @"end_cursor": assets[assets.count - 1][@"node"][@"image"][@"uri"],
      @"has_next_page": @(hasNextPage),
    },
    @"limited": @(isLimited)
  });
}

RCT_EXPORT_METHOD(getPhotos:(NSDictionary *)params
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  checkPhotoLibraryConfig();

  NSUInteger const first = [RCTConvert NSInteger:params[@"first"]];
  NSString *const afterCursor = [RCTConvert NSString:params[@"after"]];
  NSString *const groupName = [RCTConvert NSString:params[@"groupName"]];
  NSString *const groupTypes = [[RCTConvert NSString:params[@"groupTypes"]] lowercaseString];
  NSString *const mediaType = [RCTConvert NSString:params[@"assetType"]];
  NSUInteger const fromTime = [RCTConvert NSInteger:params[@"fromTime"]];
  NSUInteger const toTime = [RCTConvert NSInteger:params[@"toTime"]];
  NSArray<NSString *> *const mimeTypes = [RCTConvert NSStringArray:params[@"mimeTypes"]];
  NSArray<NSString *> *const include = [RCTConvert NSStringArray:params[@"include"]];

  BOOL __block includeFilename = [include indexOfObject:@"filename"] != NSNotFound;
  BOOL __block includeFileSize = [include indexOfObject:@"fileSize"] != NSNotFound;
  BOOL __block includeLocation = [include indexOfObject:@"location"] != NSNotFound;
  BOOL __block includeImageSize = [include indexOfObject:@"imageSize"] != NSNotFound;
  BOOL __block includePlayableDuration = [include indexOfObject:@"playableDuration"] != NSNotFound;

  // If groupTypes is "all", we want to fetch the SmartAlbum "all photos". Otherwise, all
  // other groupTypes values require the "album" collection type.
  PHAssetCollectionType const collectionType = ([groupTypes isEqualToString:@"all"]
                                                ? PHAssetCollectionTypeSmartAlbum
                                                : PHAssetCollectionTypeAlbum);
  PHAssetCollectionSubtype const collectionSubtype = [RCTConvert PHAssetCollectionSubtype:groupTypes];

  // Predicate for fetching assets within a collection
  PHFetchOptions *const assetFetchOptions = [RCTConvert PHFetchOptionsFromMediaType:mediaType fromTime:fromTime toTime:toTime];
  // We can directly set the limit if we guarantee every image fetched will be
  // added to the output array within the `collectAsset` block
  BOOL collectAssetMayOmitAsset = !!afterCursor || [mimeTypes count] > 0;
  if (!collectAssetMayOmitAsset) {
    // We set the fetchLimit to first + 1 so that `hasNextPage` will be set
    // correctly:
    // - If the user set `first: 10` and there are 11 photos, `hasNextPage`
    //   will be set to true below inside of `collectAsset`
    // - If the user set `first: 10` and there are 10 photos, `hasNextPage`
    //   will not be set, as expected
    assetFetchOptions.fetchLimit = first + 1;
  }
  assetFetchOptions.sortDescriptors = @[[NSSortDescriptor sortDescriptorWithKey:@"creationDate" ascending:NO]];

  BOOL __block foundAfter = NO;
  BOOL __block hasNextPage = NO;
  BOOL __block resolvedPromise = NO;
  NSMutableArray<NSDictionary<NSString *, id> *> *assets = [NSMutableArray new];

  // Filter collection name ("group")
  PHFetchOptions *const collectionFetchOptions = [PHFetchOptions new];
  collectionFetchOptions.sortDescriptors = @[[NSSortDescriptor sortDescriptorWithKey:@"endDate" ascending:NO]];
  if (groupName != nil) {
    collectionFetchOptions.predicate = [NSPredicate predicateWithFormat:@"localizedTitle = %@", groupName];
  }

  BOOL __block stopCollections_;
  NSString __block *currentCollectionName;

  requestPhotoLibraryAccess(reject, ^(bool isLimited){
    void (^collectAsset)(PHAsset*, NSUInteger, BOOL*) = ^(PHAsset * _Nonnull asset, NSUInteger assetIdx, BOOL * _Nonnull stopAssets) {
      NSString *const uri = [NSString stringWithFormat:@"ph://%@", [asset localIdentifier]];
       
      if (afterCursor && !foundAfter) {
        if ([afterCursor isEqualToString:uri]) {
          foundAfter = YES;
        }
        return;
      }
      NSString *_Nullable originalFilename = NULL;
      PHAssetResource *_Nullable resource = NULL;
      NSNumber* fileSize = [NSNumber numberWithInt:0];

      if (includeFilename || includeFileSize || [mimeTypes count] > 0) {
        // Get underlying resources of an asset - this includes files as well as details about edited PHAssets
        // This is required for the filename and mimeType filtering
        NSArray<PHAssetResource *> *const assetResources = [PHAssetResource assetResourcesForAsset:asset];
        resource = [assetResources firstObject];
        originalFilename = resource.originalFilename;
        fileSize = [resource valueForKey:@"fileSize"];
      }

      // WARNING: If you add any code to `collectAsset` that may skip adding an
      // asset to the `assets` output array, you should do it inside this
      // block and ensure the logic for `collectAssetMayOmitAsset` above is
      // updated
      if (collectAssetMayOmitAsset) {
        if ([mimeTypes count] > 0 && resource) {
          CFStringRef const uti = (__bridge CFStringRef _Nonnull)(resource.uniformTypeIdentifier);
          NSString *const mimeType = (NSString *)CFBridgingRelease(UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType));

          BOOL __block mimeTypeFound = NO;
          [mimeTypes enumerateObjectsUsingBlock:^(NSString * _Nonnull mimeTypeFilter, NSUInteger idx, BOOL * _Nonnull stop) {
            if ([mimeType isEqualToString:mimeTypeFilter]) {
              mimeTypeFound = YES;
              *stop = YES;
            }
          }];

          if (!mimeTypeFound) {
            return;
          }
        }
      }

      // If we've accumulated enough results to resolve a single promise
      if (first == assets.count) {
        *stopAssets = YES;
        stopCollections_ = YES;
        hasNextPage = YES;
        RCTAssert(resolvedPromise == NO, @"Resolved the promise before we finished processing the results.");
        RCTResolvePromise(resolve, assets, hasNextPage, isLimited);
        resolvedPromise = YES;
        return;
      }

      NSString *const assetMediaTypeLabel = (asset.mediaType == PHAssetMediaTypeVideo
                                            ? @"video"
                                            : (asset.mediaType == PHAssetMediaTypeImage
                                                ? @"image"
                                                : (asset.mediaType == PHAssetMediaTypeAudio
                                                  ? @"audio"
                                                  : @"unknown")));
      CLLocation *const loc = asset.location;

      [assets addObject:@{
        @"node": @{
          @"type": assetMediaTypeLabel, // TODO: switch to mimeType?
          @"group_name": currentCollectionName,
          @"image": @{
              @"uri": uri,
              @"filename": (includeFilename && originalFilename ? originalFilename : [NSNull null]),
              @"height": (includeImageSize ? @([asset pixelHeight]) : [NSNull null]),
              @"width": (includeImageSize ? @([asset pixelWidth]) : [NSNull null]),
              @"fileSize": (includeFileSize && fileSize ? fileSize : [NSNull null]),
              @"playableDuration": (includePlayableDuration && asset.mediaType != PHAssetMediaTypeImage
                                    ? @([asset duration]) // fractional seconds
                                    : [NSNull null])
          },
          @"timestamp": @(asset.creationDate.timeIntervalSince1970),
          @"location": (includeLocation && loc ? @{
              @"latitude": @(loc.coordinate.latitude),
              @"longitude": @(loc.coordinate.longitude),
              @"altitude": @(loc.altitude),
              @"heading": @(loc.course),
              @"speed": @(loc.speed), // speed in m/s
            } : [NSNull null])
          }
      }];
    };

    if ([groupTypes isEqualToString:@"all"]) {
      PHFetchResult <PHAsset *> *const assetFetchResult = [PHAsset fetchAssetsWithOptions: assetFetchOptions];
      currentCollectionName = @"All Photos";
      [assetFetchResult enumerateObjectsUsingBlock:collectAsset];
    } else {
      PHFetchResult<PHAssetCollection *> *const assetCollectionFetchResult = [PHAssetCollection fetchAssetCollectionsWithType:collectionType subtype:collectionSubtype options:collectionFetchOptions];
      [assetCollectionFetchResult enumerateObjectsUsingBlock:^(PHAssetCollection * _Nonnull assetCollection, NSUInteger collectionIdx, BOOL * _Nonnull stopCollections) {
        // Enumerate assets within the collection
        PHFetchResult<PHAsset *> *const assetsFetchResult = [PHAsset fetchAssetsInAssetCollection:assetCollection options:assetFetchOptions];
        currentCollectionName = [assetCollection localizedTitle];
        [assetsFetchResult enumerateObjectsUsingBlock:collectAsset];
        *stopCollections = stopCollections_;
      }];
    }

    // If we get this far and haven't resolved the promise yet, we reached the end of the list of photos
    if (!resolvedPromise) {
      hasNextPage = NO;
      RCTResolvePromise(resolve, assets, hasNextPage, isLimited);
      resolvedPromise = YES;
    }
  }, false);
}

RCT_EXPORT_METHOD(deletePhotos:(NSArray<NSString *>*)assets
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSMutableArray *convertedAssets = [NSMutableArray array];

  for (NSString *asset in assets) {
    [convertedAssets addObject: [asset stringByReplacingOccurrencesOfString:@"ph://" withString:@""]];
  }

  [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
      PHFetchResult<PHAsset *> *fetched =
        [PHAsset fetchAssetsWithLocalIdentifiers:convertedAssets options:nil];
      [PHAssetChangeRequest deleteAssets:fetched];
    }
  completionHandler:^(BOOL success, NSError *error) {
    if (success == YES) {
      resolve(@(success));
    }
    else {
      reject(@"Couldn't delete", @"Couldn't delete assets", error);
    }
  }
  ];
}

RCT_EXPORT_METHOD(getPhotoByInternalID:(NSString *)internalId
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  checkPhotoLibraryConfig();

  BOOL const convertHeic = [RCTConvert BOOL:options[@"convertHeicImages"]];

  requestPhotoLibraryAccess(reject, ^(bool isLimited){
    
    PHFetchResult<PHAsset *> *fetchResult;
    PHAsset *asset;
    
    NSString *mediaIdentifier = internalId;
    
    if ([internalId rangeOfString:@"ph://"].location != NSNotFound) {
      mediaIdentifier = [internalId stringByReplacingOccurrencesOfString:@"ph://"
                                                                   withString:@""];
    }
    
    fetchResult = [PHAsset fetchAssetsWithLocalIdentifiers:@[mediaIdentifier] options:nil];
    if(fetchResult){
      asset = fetchResult.firstObject;//only object in the array.
    }
    
    if(asset){
      __block NSURL *imageURL = [[NSURL alloc]initWithString:@""];
      
      NSString *const assetMediaTypeLabel = (asset.mediaType == PHAssetMediaTypeVideo
                                             ? @"video"
                                             : (asset.mediaType == PHAssetMediaTypeImage
                                                ? @"image"
                                                : (asset.mediaType == PHAssetMediaTypeAudio
                                                   ? @"audio"
                                                   : @"unknown")));


      CLLocation *const loc = asset.location;
      
      NSArray<PHAssetResource *> *const assetResources = [PHAssetResource assetResourcesForAsset:asset];
      if (![assetResources firstObject]) {
        return;
      }
      PHAssetResource *const _Nonnull resource = [assetResources firstObject];
      
      __block NSString *originalFilename = resource.originalFilename;
      NSString *const uniformMimeType = resource.uniformTypeIdentifier;
      
      __block NSString *filePath = @"";

      // check if HEIC extension asset
      if (convertHeic && asset.mediaType == PHAssetMediaTypeImage && [uniformMimeType  isEqual: @"public.heic"]) {
        // convert to JPEG
        PHImageRequestOptions *const requestOptions = [PHImageRequestOptions new];
        requestOptions.networkAccessAllowed = YES;
        requestOptions.version = PHImageRequestOptionsVersionUnadjusted;
        requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
        
        CGSize const targetSize = CGSizeMake((CGFloat)asset.pixelWidth, (CGFloat)asset.pixelHeight);
        [[PHImageManager defaultManager] requestImageForAsset:asset
                                                     targetSize:targetSize
                                                    contentMode:PHImageContentModeDefault
                                                        options:requestOptions
                                                  resultHandler:^(UIImage * _Nullable image,
                                                                  NSDictionary * _Nullable info) {
          NSError *const error = [info objectForKey:PHImageErrorKey];
          if (error) {
            reject(@"Error while converting to JPEG image",@"Error while converting",error);
          }

          originalFilename = [originalFilename stringByReplacingOccurrencesOfString:@"HEIC" withString:@"JPEG" options:NSCaseInsensitiveSearch range:NSMakeRange(0, [originalFilename length])];
          NSData *const imageData = UIImageJPEGRepresentation(image, 1.0);
          NSFileManager *fileManager = [NSFileManager defaultManager];
          NSString *fullPath = [NSTemporaryDirectory() stringByAppendingPathComponent:originalFilename];
          if ([fileManager createFileAtPath:fullPath contents:imageData attributes:nil]) {
            unsigned long long fileSize = [[fileManager attributesOfItemAtPath:fullPath error:nil] fileSize];

            resolve(@{
                      @"node": @{
                          @"type": assetMediaTypeLabel,
                          @"image": @{
                              @"filepath": fullPath,
                              @"filename": originalFilename,
                              @"height": @([asset pixelHeight]),
                              @"width": @([asset pixelWidth]),
                              @"isStored": @YES,
                              @"playableDuration": @([asset duration]), // fractional seconds
                              @"fileSize": @(fileSize)
                              },
                          @"timestamp": @(asset.creationDate.timeIntervalSince1970),
                          @"location": (loc ? @{
                                                @"latitude": @(loc.coordinate.latitude),
                                                @"longitude": @(loc.coordinate.longitude),
                                                @"altitude": @(loc.altitude),
                                                @"heading": @(loc.course),
                                                @"speed": @(loc.speed), // speed in m/s
                                                } : @{})
                          }
                      });
          } else {
            NSString *errorMessage = [NSString stringWithFormat:@"Failed to create tmp file for asset %@.", originalFilename];
            NSError *error = RCTErrorWithMessage(errorMessage);
            reject(@"Error while creating image tmp file",@"Error creating tmp file",error);
          }

        }];
      } else {
        NSNumber* fileSize = [resource valueForKey:@"fileSize"];
        PHContentEditingInputRequestOptions *const editOptions = [PHContentEditingInputRequestOptions new];
        // Download asset if on icloud.
        editOptions.networkAccessAllowed = YES;
        
        [asset requestContentEditingInputWithOptions:editOptions completionHandler:^(PHContentEditingInput *contentEditingInput, NSDictionary *info) {
          imageURL = contentEditingInput.fullSizeImageURL;
          if (imageURL.absoluteString.length != 0) {
            
            filePath = [imageURL.absoluteString stringByReplacingOccurrencesOfString:@"pathfile:" withString:@"file:"];

            resolve(@{
                      @"node": @{
                          @"type": assetMediaTypeLabel,
                          @"image": @{
                              @"filepath": filePath,
                              @"filename": originalFilename,
                              @"height": @([asset pixelHeight]),
                              @"width": @([asset pixelWidth]),
                              @"isStored": @YES,
                              @"playableDuration": @([asset duration]), // fractional seconds
                              @"fileSize": fileSize
                              },
                          @"timestamp": @(asset.creationDate.timeIntervalSince1970),
                          @"location": (loc ? @{
                                                @"latitude": @(loc.coordinate.latitude),
                                                @"longitude": @(loc.coordinate.longitude),
                                                @"altitude": @(loc.altitude),
                                                @"heading": @(loc.course),
                                                @"speed": @(loc.speed), // speed in m/s
                                                } : @{})
                          }
                      });
          } else {
            NSString *errorMessage = [NSString stringWithFormat:@"Failed to load asset"
                                      " with localIdentifier %@ with no error message.", internalId];
            NSError *error = RCTErrorWithMessage(errorMessage);
            reject(@"Error while getting file path",@"Error while getting file path",error);
          }
        }];
      }
      
    } else {
      NSString *errorMessage = [NSString stringWithFormat:@"Failed to load asset"
                                " with localIdentifier %@ with no error message.", internalId];
      NSError *error = RCTErrorWithMessage(errorMessage);
      reject(@"No asset found",@"No asset found",error);
    }
    
  }, false);
}

static void checkPhotoLibraryConfig()
{
#if RCT_DEV
  if (![[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSPhotoLibraryUsageDescription"]) {
    RCTLogError(@"NSPhotoLibraryUsageDescription key must be present in Info.plist to use camera roll.");
  }
#endif
}

@end
