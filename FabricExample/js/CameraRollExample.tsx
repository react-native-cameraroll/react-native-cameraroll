/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow
 * @format
 */
'use strict';

const React = require('react');
const ReactNative = require('react-native');
const {Image, StyleSheet, Switch, Text, View, TouchableOpacity, Dimensions} =
  ReactNative;
import {CameraRoll} from '@react-native-camera-roll/camera-roll';
import Slider from '@react-native-community/slider';
import type {
  PhotoIdentifier,
  GroupTypes,
} from '@react-native-camera-roll/camera-roll';
import { Platform } from 'react-native';

const invariant = require('invariant');

const CameraRollView = require('./CameraRollView');

type Props = Readonly<{
  navigator?: Array<
    Readonly<{
      title: string;
      component: React.Component<any, any>;
      backButtonTitle: string;
      passProps: Readonly<{asset: PhotoIdentifier}>;
    }>
  >;
}>;

type State = {
  groupTypes: GroupTypes;
  sliderValue: number;
  bigImages: boolean;
};

export default class CameraRollExample extends React.Component<Props, State> {
  state = {
    groupTypes: 'All',
    sliderValue: 1,
    bigImages: false,
  };
  _cameraRollView: React.ElementRef<typeof CameraRollView>;

  render() {
    return (
      <View style={styles.flex1}>
        <View style={styles.header}>
          <Switch
            onValueChange={this._onSwitchChange}
            value={this.state.bigImages}
          />
          <Text>{(this.state.bigImages ? 'Big' : 'Small') + ' Images'}</Text>
          <Slider
            value={this.state.sliderValue}
            onValueChange={this._onSliderChange}
          />
          <Text>{'Group Type: ' + this.state.groupTypes}</Text>
        </View>
        <CameraRollView
          ref={(ref: typeof CameraRollView) => {
            this._cameraRollView = ref;
          }}
          batchSize={20}
          groupTypes={this.state.groupTypes}
          renderImage={this._renderImage}
          bigImages={this.state.bigImages}
        />
      </View>
    );
  }

  loadAsset(asset: PhotoIdentifier) {
    if (Platform.OS === 'ios') {
      CameraRoll.iosGetImageDataById(asset.node.id, {
        convertHeicImages: true,
        quality: 1,
      }).then(console.log);
    } else {
      console.log(console.log(asset));
    }
  }

  _renderImage = (asset: PhotoIdentifier) => {
    const imageSize = this.state.bigImages ? 150 : 75;
    const imageStyle = [styles.image, {width: imageSize, height: imageSize}];
    const {location} = asset.node;
    const locationStr = location
      ? JSON.stringify(location)
      : 'Unknown location';
    return (
      <TouchableOpacity
        key={asset.node.image.uri}
        onPress={this.loadAsset.bind(this, asset)}
        style={styles.flex1}>
        <View style={styles.row}>
          <Image source={{uri: asset.node.image.uri}} style={imageStyle} />
          <View style={styles.flex1}>
            <Text style={styles.url}>{asset.node.image.uri}</Text>
            <Text>{locationStr}</Text>
            <Text>{asset.node.group_name}</Text>
            <Text>{new Date(asset.node.timestamp * 1000).toString()}</Text>
            <Text>
              {new Date(asset.node.modificationTimestamp * 1000).toString()}
            </Text>
            <Text>Subtypes: {asset.node.subTypes}</Text>
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  _onSliderChange = (value: number) => {
    const options = Object.keys(CameraRoll.GroupTypesOptions);
    const index = Math.floor(value * options.length * 0.99);
    const groupTypes = options[index];
    if (groupTypes !== this.state.groupTypes) {
      this.setState({groupTypes: groupTypes});
    }
  };

  _onSwitchChange = (value: number) => {
    invariant(this._cameraRollView, 'ref should be set');
    this.setState({bigImages: value});
  };
}

const styles = StyleSheet.create({
  header: {
    padding: 20,
    width: Dimensions.get('window').width,
  },
  row: {
    flexDirection: 'row',
    flex: 1,
    width: '100%',
  },
  url: {
    fontSize: 9,
    marginBottom: 14,
  },
  image: {
    margin: 4,
  },
  flex1: {
    flex: 1,
  },
});
