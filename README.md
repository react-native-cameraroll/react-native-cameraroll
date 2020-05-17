# `@react-native-community/cameraroll`

[![CircleCI Status][circle-ci-badge]][circle-ci]
![Supports Android and iOS][supported-os-badge]
![MIT License][license-badge]
[![Lean Core Badge][lean-core-badge]][lean-core-issue]

## Getting started

`$ npm install @react-native-community/cameraroll --save`

### Mostly automatic installation

`$ react-native link @react-native-community/cameraroll && npx pod-install`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `@react-native-community/cameraroll` and add `RNCCameraroll.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNCCameraroll.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.reactnativecommunity.cameraroll.CameraRollPackage;` to the imports at the top of the file
  - Add `new CameraRollPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':@react-native-community_cameraroll'
  	project(':@react-native-community_cameraroll').projectDir = new File(rootProject.projectDir, 	'../node_modules/@react-native-community/cameraroll/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':@react-native-community_cameraroll')
  	```

## Migrating from the core `react-native` module
This module was created when the CameraRoll was split out from the core of React Native. To migrate to this module you need to follow the installation instructions above and then change you imports from:

```javascript
import { CameraRoll } from "react-native";
```

to:

```javascript
import CameraRoll from "@react-native-community/cameraroll";
```

## Usage

`CameraRoll` provides access to the local camera roll or photo library.

### Permissions

The user's permission is required in order to access the Camera Roll on devices running iOS 10 or later. Add the `NSPhotoLibraryUsageDescription` key in your `Info.plist` with a string that describes how your app will use this data. This key will appear as `Privacy - Photo Library Usage Description` in Xcode.

If you are targeting devices running iOS 11 or later, you will also need to add the `NSPhotoLibraryAddUsageDescription` key in your `Info.plist`. Use this key to define a string that describes how your app will use this data. By adding this key to your `Info.plist`, you will be able to request write-only access permission from the user. If you try to save to the camera roll without this permission, your app will exit.

On Android permission is required to read the external storage. Add below line to your manifest to request this permission on app install.

```
<manifest>
...
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
...
<application>
```

### Methods

* [`save`](#save)
* [`getAlbums`](#getalbums)
* [`getPhotos`](#getphotos)
* [`getPhotosFast`](#getphotosfast)
* [`deletePhotos`](#deletephotos)

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

If the tag has a file extension of .mov or .mp4, it will be inferred as a video. Otherwise it will be treated as a photo. To override the automatic choice, you can pass an optional `type` parameter that must be one of 'photo' or 'video'.

It allows to specify a particular album you want to store the asset to.

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

**Returns:**

Array of `Album` object
  * title: {string}
  * count: {number}

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
* `after` : {string} : A cursor that matches `page_info { end_cursor }` returned from a previous call to `getPhotos`.
* `groupTypes` : {string} : Specifies which group types to filter the results to. Valid values are:
  * `Album`
  * `All` // default
  * `Event`
  * `Faces`
  * `Library`
  * `PhotoStream`
  * `SavedPhotos`
* `groupName` : {string} : Specifies filter on group names, like 'Recent Photos' or custom album titles.
* `assetType` : {string} : Specifies filter on asset type. Valid values are:
  * `All`
  * `Videos`
  * `Photos` // default
* `mimeTypes` : {Array} : Filter by mimetype (e.g. image/jpeg).
* `fromTime` : {timestamp} : Filter from date added.
* `toTime` : {timestamp} : Filter to date added.

Returns a Promise which when resolved will be of the following shape:

* `edges` : {Array<node>} An array of node objects
  * `node`: {object} An object with the following shape:
    * `type`: {string}
    * `group_name`: {string}
    * `image`: {object} : An object with the following shape:
      * `uri`: {string}
      * `filename`: {string}
      * `height`: {number}
      * `width`: {number}
      * `fileSize`: {number}
      * `isStored`: {boolean}
      * `playableDuration`: {number}
    * `timestamp`: {number}
    * `location`: {object} : An object with the following shape:
      * `latitude`: {number}
      * `longitude`: {number}
      * `altitude`: {number}
      * `heading`: {number}
      * `speed`: {number}
* `page_info` : {object} : An object with the following shape:
  * `has_next_page`: {boolean}
  * `start_cursor`: {string}
  * `end_cursor`: {string}

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

---

### `getPhotosFast()`

```javascript
CameraRoll.getPhotosFast(params);
```

This function is largely the same as the [`getPhotos`](#getphotos) function, but does not return the filename on iOS. As a result, it can be much faster there. On a Camera Roll of 1000 photos, it will take 0.2 instead of 5.0 seconds.

The `mimeTypes` and `after` parameters in `getPhotos` are also unavailable.

**Parameters:**

| Name   | Type   | Required | Description                                      |
| ------ | ------ | -------- | ------------------------------------------------ |
| params | object | Yes      | Expects a params with the shape described below. |

All parameters have the same types and arguments as the parameters of the same names in [`getPhotos`](#getphotos).

* `first` : {number}
* `groupTypes` : {string}
* `groupName` : {string}
* `assetType` : {string}
* `fromTime` : {timestamp}
* `toTime` : {timestamp}

Returns a Promise which resolves with the same object as [`getPhotos`](#getphotos).

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


[circle-ci-badge]:https://img.shields.io/circleci/project/github/react-native-community/react-native-cameraroll/master.svg?style=flat-square
[circle-ci]:https://circleci.com/gh/react-native-community/workflows/react-native-cameraroll/tree/master
[supported-os-badge]:https://img.shields.io/badge/platforms-android%20|%20ios-lightgrey.svg?style=flat-square
[license-badge]:https://img.shields.io/npm/l/@react-native-community/cameraroll.svg?style=flat-square
[lean-core-badge]: https://img.shields.io/badge/Lean%20Core-Extracted-brightgreen.svg?style=flat-square
[lean-core-issue]: https://github.com/facebook/react-native/issues/23313
