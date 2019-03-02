/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 * @lint-ignore-every XPLATJSCOPYRIGHT1
 */

import React, {Component} from 'react';
import {StyleSheet, Text, View} from 'react-native';
import CameraRoll from '@react-native-community/cameraroll';
import CameraRollExample from './js/CameraRollExample';

type Props = {};
export default class App extends Component<Props> {
  componentDidMount = async () => {
    const photos = await CameraRoll.getPhotos({first: 0, assetType: 'Photos'});
  };

  render() {
    return (
      <View style={styles.container}>
        <CameraRollExample />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
