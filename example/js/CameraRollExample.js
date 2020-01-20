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
const {
  Image,
  Slider,
  StyleSheet,
  Switch,
  Text,
  View,
  TouchableOpacity,
  Dimensions,
} = ReactNative;
import CameraRoll from '../../js/CameraRoll';
import type {PhotoIdentifier, GroupTypes} from '../../js/CameraRoll';

const invariant = require('fbjs/lib/invariant');

const CameraRollView = require('./CameraRollView');

const AssetScaledImageExampleView = require('./AssetScaledImageExample');

type Props = $ReadOnly<{|
  navigator?: ?Array<
    $ReadOnly<{|
      title: string,
      component: Class<React.Component<any, any>>,
      backButtonTitle: string,
      passProps: $ReadOnly<{|asset: PhotoIdentifier|}>,
    |}>,
  >,
|}>;

type State = {|
  groupTypes: GroupTypes,
  sliderValue: number,
  bigImages: boolean,
|};

export default class CameraRollExample extends React.Component<Props, State> {
  state = {
    groupTypes: 'All',
    sliderValue: 1,
    bigImages: true,
  };
  _cameraRollView: ?React.ElementRef<typeof CameraRollView>;

  render() {
    return (
      <View>
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
          ref={ref => {
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
    if (this.props.navigator) {
      this.props.navigator.push({
        title: 'Camera Roll Image',
        component: AssetScaledImageExampleView,
        backButtonTitle: 'Back',
        passProps: {asset: asset},
      });
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
        onPress={this.loadAsset.bind(this, asset)}>
        <View style={styles.row}>
          <Image source={asset.node.image} style={imageStyle} />
          <View style={styles.info}>
            <Text style={styles.url}>{asset.node.image.uri}</Text>
            <Text>{locationStr}</Text>
            <Text>{asset.node.group_name}</Text>
            <Text>{new Date(asset.node.timestamp * 1000).toString()}</Text>
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  _onSliderChange = value => {
    const options = Object.keys(CameraRoll.GroupTypesOptions);
    const index = Math.floor(value * options.length * 0.99);
    const groupTypes = options[index];
    if (groupTypes !== this.state.groupTypes) {
      this.setState({groupTypes: groupTypes});
    }
  };

  _onSwitchChange = value => {
    invariant(this._cameraRollView, 'ref should be set');
    this.setState({bigImages: value});
  };
}

const styles = StyleSheet.create({
  header: {
    marginTop: 44,
    padding: 20,
    width: Dimensions.get('window').width,
  },
  row: {
    flexDirection: 'row',
    flex: 1,
  },
  url: {
    fontSize: 9,
    marginBottom: 14,
  },
  image: {
    margin: 4,
  },
  info: {
    flex: 1,
  },
});
