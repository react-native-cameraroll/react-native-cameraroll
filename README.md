# `@react-native-camera-roll/camera-roll`

[![CircleCI Status][circle-ci-badge]][circle-ci]
![Supports Android and iOS][supported-os-badge]
![MIT License][license-badge]
[![Lean Core Badge][lean-core-badge]][lean-core-issue]

## *Notice*: The NPM package name has changed, please change your package.json dependency! 

Previous package name: @react-native-community/cameraroll

New package name: @react-native-camera-roll/camera-roll


## Getting started

`$ npm install @react-native-camera-roll/camera-roll --save`

### Mostly automatic installation

`$ react-native link @react-native-camera-roll/camera-roll && npx pod-install`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `@react-native-camera-roll/camera-roll` and add `RNCCameraroll.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNCCameraroll.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java` (Auto link, ^RN0.69 does not required)
  - Add `import com.reactnativecommunity.cameraroll.CameraRollPackage;` to the imports at the top of the file
  - Add `new CameraRollPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':@react-native-camera-roll_camera-roll'
  	project(':@react-native-camera-roll_camera-roll').projectDir = new File(rootProject.projectDir, 	'../node_modules/@react-native-camera-roll/camera-roll/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      implementation project(':@react-native-camera-roll_camera-roll')
  	```

## Migrating from the core `react-native` module
This module was created when the CameraRoll was split out from the core of React Native. To migrate to this module you need to follow the installation instructions above and then change you imports from:

```javascript
import { CameraRoll } from "react-native";
```

to:

```javascript
import { CameraRoll } from "@react-native-camera-roll/camera-roll";
```

## Usage

`CameraRoll` provides access to the local camera roll or photo library.

### Permissions

**iOS**

The user's permission is required in order to access the Camera Roll on devices running iOS 10 or later. Add the `NSPhotoLibraryUsageDescription` key in your `Info.plist` with a string that describes how your app will use this data. This key will appear as `Privacy - Photo Library Usage Description` in Xcode.

If you are targeting devices running iOS 11 or later, you will also need to add the `NSPhotoLibraryAddUsageDescription` key in your `Info.plist`. Use this key to define a string that describes how your app will use this data. By adding this key to your `Info.plist`, you will be able to request write-only access permission from the user. If you try to save to the camera roll without this permission, your app will exit.

**Android**

Permission is required to read and write to the external storage.

On Expo, follow the guide [here](https://docs.expo.io/versions/latest/sdk/permissions/) for requesting the permission.

On react-native-cli or ejected apps, adding the following lines will add the capability for the app to request the permission. Find more info on Android Permissions [here](https://reactnative.dev/docs/permissionsandroid).

```xml
<manifest>
...
  <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
  <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
...
<application>
```

Then you have to explicitly ask for the permission

```javascript
import { PermissionsAndroid, Platform } from "react-native";
import { CameraRoll } from "@react-native-camera-roll/camera-roll";

async function hasAndroidPermission() {
  const getCheckPermissionPromise = () => {
    if (Platform.Version >= 33) {
      return Promise.all([
        PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES),
        PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.READ_MEDIA_VIDEO),
      ]).then(
        ([hasReadMediaImagesPermission, hasReadMediaVideoPermission]) =>
          hasReadMediaImagesPermission && hasReadMediaVideoPermission,
      );
    } else {
      return PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE);
    }
  };

  const hasPermission = await getCheckPermissionPromise();
  if (hasPermission) {
    return true;
  }
  const getRequestPermissionPromise = () => {
    if (Platform.Version >= 33) {
      return PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES,
        PermissionsAndroid.PERMISSIONS.READ_MEDIA_VIDEO,
      ]).then(
        (statuses) =>
          statuses[PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES] ===
            PermissionsAndroid.RESULTS.GRANTED &&
          statuses[PermissionsAndroid.PERMISSIONS.READ_MEDIA_VIDEO] ===
            PermissionsAndroid.RESULTS.GRANTED,
      );
    } else {
      return PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE).then((status) => status === PermissionsAndroid.RESULTS.GRANTED);
    }
  };

  return await getRequestPermissionPromise();
}

async function savePicture() {
  if (Platform.OS === "android" && !(await hasAndroidPermission())) {
    return;
  }

  CameraRoll.save(tag, { type, album })
};
```

### Methods

* [`save`](#save)
* [`getAlbums`](#getalbums)
* [`getPhotos`](#getphotos)
* [`deletePhotos`](#deletephotos)
* [`iosGetImageDataById`](#iosgetimagedatabyid)
* [`useCameraRoll`](#usecameraroll)
* [`getPhotoThumbnail`](#getphotothumbnail) **iOS only**

---

# Reference

## Methods

### `save()`

```javascript
CameraRoll.save(tag, { type, album })
```

Saves the photo or video to the photo library.

On Android, the tag must be a local image or video URI, such as `"file:///sdcard/img.png"`.

On iOS, the tag can be any image URI (including local, remote asset-library and base64 data URIs) or a local video file URI (remote or data URIs are not supported for saving video at this time).

If the tag has a file extension of .mov or .mp4 (lower or uppercase), it will be inferred as a video. Otherwise it will be treated as a photo. To override the automatic choice, you can pass an optional `type` parameter that must be one of 'photo' or 'video'.

It allows to specify a particular album you want to store the asset to when the param `album` is provided.
On Android, if no album is provided, DCIM directory is used, otherwise PICTURE or MOVIES directory is used depending on the `type` provided.

Returns a Promise which will resolve with the new URI.

**Parameters:**

| Name | Type                   | Required | Description                                                |
| ---- | ---------------------- | -------- | ---------------------------------------------------------- |
| tag  | string                 | Yes      | See above.                                                 |
| type | enum('photo', 'video') | No       | Overrides automatic detection based on the file extension. |
| album | string                | No       | The album to save to |

---
### `getAlbums()`

```javascript
CameraRoll.getAlbums(params);
```
Returns a Promise with a list of albums

**Parameters:**

* `assetType` : {string} : Specifies filter on asset type. Valid values are:
  * `All` // default
  * `Videos`
  * `Photos`
* `albumType` : {string} :  (iOS only) Specifies filter on type of album. Valid values are:
  * `All`
  * `Album` // default
  * `SmartAlbum`

**Returns:**

Array of `Album` object
  * title: {string}
  * count: {number}
  * type: {string} (iOS only)
  * subtype: {string | undefined} : See AlbumSubType type for possible values. iOS only.

---

### `getPhotos()`

```javascript
CameraRoll.getPhotos(params);
```

Returns a Promise with photo identifier objects from the local camera roll of the device matching shape defined by `getPhotosReturnChecker`.

**Parameters:**

| Name   | Type   | Required | Description                                      |
| ------ | ------ | -------- | ------------------------------------------------ |
| params | object | Yes      | Expects a params with the shape described below. |

* `first` : {number} : The number of photos wanted in reverse order of the photo application (i.e. most recent first for SavedPhotos). Required.
* `after` : {string} : A cursor that matches `page_info { end_cursor }` returned from a previous call to `getPhotos`. Note that using this will reduce performance slightly on iOS. An alternative is just using the `fromTime` and `toTime` filters, which have no such impact.
* `groupTypes` : {string} : Specifies which group types to filter the results to. Valid values are:
  * `Album`
  * `All` // default
  * `Event`
  * `Faces`
  * `Library`
  * `SmartAlbum`
  * `PhotoStream`
  * `SavedPhotos`
* `groupName` : {string} : Specifies filter on group names, like 'Recent Photos' or custom album titles.
* `includeSharedAlbums` : {boolean} : Include assets originating from an iCloud Shared Album. iOS only.
* `assetType` : {string} : Specifies filter on asset type. Valid values are:
  * `All`
  * `Videos`
  * `Photos` // default
* `mimeTypes` : {Array} : Filter by mimetype (e.g. image/jpeg). Note that using this will reduce performance slightly on iOS.
* `fromTime` : {number} : Filter by creation time with a timestamp in milliseconds. This time is exclusive, so we'll select all photos with `timestamp > fromTime`.
* `toTime` : {number} : Filter by creation time with a timestamp in milliseconds. This time is inclusive, so we'll select all photos with `timestamp <= toTime`.
* `include` : {Array} : Whether to include some fields that are slower to fetch
  * `filename` : Ensures `image.filename` is available in each node. This has a large performance impact on iOS.
  * `fileSize` : Ensures `image.fileSize` is available in each node. This has a large performance impact on iOS.
  * `fileExtension` : Ensures `image.fileExtension` is available in each node.
  * `location`: Ensures `location` is available in each node. This has a large performance impact on Android.
  * `imageSize` : Ensures `image.width` and `image.height` are available in each node. This has a small performance impact on Android.
  * `playableDuration` : Ensures `image.playableDuration` is available in each node. This has a medium peformance impact on Android.
  * `orientation` : Ensures `image.orientation` is available in each node. This has a small peformance impact on Android. **Android only**
  * `albums` : Ensures `group_name` is available in each node. This has a large peformance impact on iOS.

Returns a Promise which when resolved will be of the following shape:

* `edges` : {Array<node>} An array of node objects
  * `node`: {object} An object with the following shape:
    * `id`: {string} : A local identifier. Correspond to `Media._ID` on Android and `localIdentifier` on iOS.
    * `type`: {string}
    * `subTypes`: {Array<string>} : An array of subtype strings (see `SubTypes` type). Always [] on Android.
    * `group_name`: {Array<string>} : An array of albums containing the element. Always 1 element on Android. 0 to n elements on iOS.
    * `image`: {object} : An object with the following shape:
      * `uri`: {string}
      * `filename`: {string | null} : Only set if the `include` parameter contains `filename`
      * `extension`: {string | null} : Only set if the `include` parameter contains `fileExtension`
      * `height`: {number | null} : Only set if the `include` parameter contains `imageSize`
      * `width`: {number | null} : Only set if the `include` parameter contains `imageSize`
      * `fileSize`: {number | null} : Only set if the `include` parameter contains `fileSize`
      * `playableDuration`: {number | null} : Only set for videos if the `include` parameter contains `playableDuration`. Will be null for images.
      * `orientation`: {number | null} : Only set for images if the `include` parameter contains `orientation`. **Android only**
    * `timestamp`: {number}
    * `modificationTimestamp`: {number}
    * `location`: {object | null} : Only set if the `include` parameter contains `location`. An object with the following shape:
      * `latitude`: {number}
      * `longitude`: {number}
      * `altitude`: {number}
      * `heading`: {number}
      * `speed`: {number}
* `page_info` : {object} : An object with the following shape:
  * `has_next_page`: {boolean}
  * `start_cursor`: {string}
  * `end_cursor`: {string}
* `limited` : {boolean | undefined} : true if the app can only access a subset of the gallery pictures (authorization is `PHAuthorizationStatusLimited`), false otherwise (iOS only)

#### Example

Loading images:

```javascript
_handleButtonPress = () => {
   CameraRoll.getPhotos({
       first: 20,
       assetType: 'Photos',
     })
     .then(r => {
       this.setState({ photos: r.edges });
     })
     .catch((err) => {
        //Error Loading Images
     });
   };
render() {
 return (
   <View>
     <Button title="Load Images" onPress={this._handleButtonPress} />
     <ScrollView>
       {this.state.photos.map((p, i) => {
       return (
         <Image
           key={i}
           style={{
             width: 300,
             height: 100,
           }}
           source={{ uri: p.node.image.uri }}
         />
       );
     })}
     </ScrollView>
   </View>
 );
}
```

Loading images with listeners and refetchs:

```javascript
import { CameraRoll, cameraRollEventEmitter } from '@react-native-camera-roll/camera-roll';

import { useCallback, useEffect, useState } from 'react';

import { AppState, EmitterSubscription } from 'react-native';

interface GalleryOptions {
  pageSize: number;
  mimeTypeFilter?: Array<string>;
}

interface GalleryLogic {
  photos?: ImageDTO[];
  loadNextPagePictures: () => void;
  isLoading: boolean;
  isLoadingNextPage: boolean;
  isReloading: boolean;
  hasNextPage: boolean;
}

const supportedMimeTypesByTheBackEnd = [
  'image/jpeg',
  'image/png',
  'image/heif',
  'image/heic',
  'image/heif-sequence',
  'image/heic-sequence',
];

export const useGallery = ({
  pageSize = 30,
  mimeTypeFilter = supportedMimeTypesByTheBackEnd,
}: GalleryOptions): GalleryLogic => {
  const [isLoading, setIsLoading] = useState(false);
  const [isReloading, setIsReloading] = useState(false);
  const [isLoadingNextPage, setIsLoadingNextPage] = useState(false);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [nextCursor, setNextCursor] = useState<string>();
  const [photos, setPhotos] = useState<ImageDTO[]>();

  const loadNextPagePictures = useCallback(async () => {
    try {
      nextCursor ? setIsLoadingNextPage(true) : setIsLoading(true);
      const { edges, page_info } = await CameraRoll.getPhotos({
        first: pageSize,
        after: nextCursor,
        assetType: 'Photos',
        mimeTypes: mimeTypeFilter,
        ...(isAndroid && { include: ['fileSize', 'filename'] }),
      });
      const photos = convertCameraRollPicturesToImageDtoType(edges);
      setPhotos((prev) => [...(prev ?? []), ...photos]);

      setNextCursor(page_info.end_cursor);
      setHasNextPage(page_info.has_next_page);
    } catch (error) {
      console.error('useGallery getPhotos error:', error);
    } finally {
      setIsLoading(false);
      setIsLoadingNextPage(false);
    }
  }, [mimeTypeFilter, nextCursor, pageSize]);

  const getUnloadedPictures = useCallback(async () => {
    try {
      setIsReloading(true);
      const { edges, page_info } = await CameraRoll.getPhotos({
        first: !photos || photos.length < pageSize ? pageSize : photos.length,
        assetType: 'Photos',
        mimeTypes: mimeTypeFilter,
        // Include fileSize only for android since it's causing performance issues on IOS.
        ...(isAndroid && { include: ['fileSize', 'filename'] }),
      });
      const newPhotos = convertCameraRollPicturesToImageDtoType(edges);
      setPhotos(newPhotos);

      setNextCursor(page_info.end_cursor);
      setHasNextPage(page_info.has_next_page);
    } catch (error) {
      console.error('useGallery getNewPhotos error:', error);
    } finally {
      setIsReloading(false);
    }
  }, [mimeTypeFilter, pageSize, photos]);

  useEffect(() => {
    if (!photos) {
      loadNextPagePictures();
    }
  }, [loadNextPagePictures, photos]);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', async (nextAppState) => {
      if (nextAppState === 'active') {
        getUnloadedPictures();
      }
    });

    return () => {
      subscription.remove();
    };
  }, [getUnloadedPictures]);

  useEffect(() => {
    let subscription: EmitterSubscription;
    if (isAboveIOS14) {
      subscription = cameraRollEventEmitter.addListener('onLibrarySelectionChange', (_event) => {
        getUnloadedPictures();
      });
    }

    return () => {
      if (isAboveIOS14 && subscription) {
        subscription.remove();
      }
    };
  }, [getUnloadedPictures]);

  return {
    photos,
    loadNextPagePictures,
    isLoading,
    isLoadingNextPage,
    isReloading,
    hasNextPage,
  };
};
```

---

### `deletePhotos()`

```javascript
CameraRoll.deletePhotos([uri]);
```

Requests deletion of photos in the camera roll.

On Android, the uri must be a local image or video URI, such as `"file:///sdcard/img.png"`.

On iOS, the uri can be any image URI (including local, remote asset-library and base64 data URIs) or a local video file URI. The user is presented with a dialog box that shows them the asset(s) and asks them to confirm deletion. This is not able to be bypassed as per Apple Developer guidelines. 

Returns a Promise which will resolve when the deletion request is completed, or reject if there is a problem during the deletion. On iOS the user is able to cancel the deletion request, which causes a rejection, while on Android the rejection will be due to a system error.

**Parameters:**

| Name | Type                   | Required | Description                                                |
| ---- | ---------------------- | -------- | ---------------------------------------------------------- |
| uri  | string                 | Yes      | See above.                                                 |


### `iosGetImageDataById()`
```javascript
CameraRoll.iosGetImageDataById(internalID, true);
```

**Parameters:**

| Name         | Type                    | Required   | Description                                               |
| ------------ | ----------------------- | ---------- | --------------------------------------------------------- |
| internalID   | string                  | Yes        | Ios internal ID 'PH://xxxx'.                              |
| options      | PhotoConvertionOptions  | False      | Expects an options object with the shape described below. |

* `convertHeic` : {boolean} : **default = false** : Whether to convert or not to JPEG image.
* `quality` : {number} : **default = 1.0** : jpeg quality used for compression (a value from 0.0 to 1.0).  A value of 0.0 is maximum compression (or lowest quality).  A value of 1.0 is least compression (or best quality).

Upload photo/video with `iosGetImageDataById` method

```javascript

try {
// uri 'PH://xxxx'          
const fileData = await CameraRoll.iosGetImageDataById(uri);
if (!fileData?.node?.image?.filepath) return undefined;
const uploadPath = imageData.node.image.filepath; // output should be file://...
// fetch or ReactNativeBlobUtil.fetch to upload 
}
catch (error) {}

```          
          


### `useCameraRoll()`

`useCameraRoll` is a utility hooks for the CameraRoll module.

```javascript
import React, {useEffect} from 'react';
import {Button} from 'react-native';
import {useCameraRoll} from "@react-native-camera-roll/camera-roll";

function Example() {
  const [photos, getPhotos, save] = useCameraRoll();

  return <>
    <Button title='Get Photos' onPress={() => getPhotos()}>Get Photos</Button>
    {
      photos.map((photo, index) => /* render photos */)
    }
  </>;
};
```


### `getPhotoThumbnail()`

**iOS only**

Returns a Promise with thumbnail photo.

**Parameters:**

| Name         | Type                  | Required | Description                                               |
| ------------ | --------------------- | -------- | --------------------------------------------------------- |
| internalID   | string                | Yes      | Ios internal ID 'PH://xxxx'.                              |
| options      | PhotoThumbnailOptions | Yes      | Expects an options object with the shape described below. |

* `allowNetworkAccess` : {boolean} : **default = false** : Specifies whether the requested image can be downloaded from iCloud. **iOS only**
* `targetSize` : {ThumbnailSize} : Expects a targetSize with the shape desribed below:
  * `height` : {number} : **default = 400**
  * `width` : {number} : **default = 400**
* `quality` : {number} : **default = 1.0** : jpeg quality used for compression (a value from 0.0 to 1.0).  A value of 0.0 is maximum compression (or lowest quality).  A value of 1.0 is least compression (or best quality).

**Returns:**

| Type                      | Description                                                   |
| ------------------------- | ------------------------------------------------------------- |
| Promise\<PhotoThumbnail\> | A Promise with PhotoThumbnail with the shape described below. |

* `thumbnailBase64` : {string}

#### Example

Loading a thumbnail:

```javascript
export default function Thumbnail(props) {
  const [base64Image, setBase64Image] = useState(null);

  useEffect(() => {
    const getThumbnail = async () => {
      const options = {
        allowNetworkAccess: true,
        targetSize: {
          height: 80,
          width: 80
        },
        quality: 1.0
      };

      const thumbnailResponse = await CameraRoll.getPhotoThumbnail(props.image.uri, options);

      setBase64Image(thumbnailResponse.thumbnailBase64);
    };

    getThumbnail();
  }, []);

  const extension = props.image.extension;
  let prefix;

  switch (extension) {
    case 'png':
      prefix = 'data:image/png;base64,';
      break;
    default:
      //all others can use jpeg
      prefix = 'data:image/jpeg;base64,';
      break;
  }

  return (
    <Image
      source={{ uri: `${prefix}${base64Image}` }}
    />
  );
}
```

### Known issues

#### IOS

If you try to save media into specific album without asking for read and write permission then saving will not work, workaround is to not precice album name for IOS if you don't want to request full permission (Only ios >= 14).

[circle-ci-badge]:https://img.shields.io/circleci/project/github/react-native-cameraroll/react-native-cameraroll/master.svg?style=flat-square
[circle-ci]:https://circleci.com/gh/react-native-cameraroll/workflows/react-native-cameraroll/tree/master
[supported-os-badge]:https://img.shields.io/badge/platforms-android%20|%20ios-lightgrey.svg?style=flat-square
[license-badge]:https://img.shields.io/npm/l/@react-native-camera-roll/camera-roll.svg?style=flat-square
[lean-core-badge]: https://img.shields.io/badge/Lean%20Core-Extracted-brightgreen.svg?style=flat-square
[lean-core-issue]: https://github.com/facebook/react-native/issues/23313
