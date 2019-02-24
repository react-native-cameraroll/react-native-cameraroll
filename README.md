
# react-native-cameraroll

## Getting started

`$ npm install @react-native-community/cameraroll --save`

### Mostly automatic installation

`$ react-native link @react-native-community/cameraroll`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `@react-native-community/cameraroll` and add `RNCCameraroll.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNCCameraroll.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactnativecommunity.cameraroll.RNCCamerarollPackage;` to the imports at the top of the file
  - Add `new RNCCamerarollPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':@react-native-community/cameraroll'
  	project(':@react-native-community/cameraroll').projectDir = new File(rootProject.projectDir, 	'../node_modules/@react-native-community/cameraroll/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':@react-native-community/cameraroll')
  	```


## Usage
```javascript
import RNCCameraroll from '@react-native-community/cameraroll';

// TODO: What to do with the module?
RNCCameraroll;
```
  