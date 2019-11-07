/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

const React = require('react');
const ReactNative = require('react-native');
const {
  ActivityIndicator,
  Alert,
  Image,
  FlatList,
  PermissionsAndroid,
  Platform,
  StyleSheet,
  View,
} = ReactNative;

import CameraRoll from '../../js/CameraRoll';

const groupByEveryN = function groupByEveryN(num) {
  const n = num;
  return arrayArg => {
    const array = [...arrayArg];
    const result = [];
    while (array.length > 0) {
      const groupByNumber = array.length >= n ? n : array.length;
      result.push(array.splice(0, groupByNumber));
    }
    return result;
  };
};
const logError = console.error;

class CameraRollView extends React.Component {
  static defaultProps = {
    groupTypes: 'All',
    batchSize: 5,
    imagesPerRow: 1,
    assetType: 'Photos',
    renderImage: function(asset) {
      const imageSize = 150;
      const imageStyle = [styles.image, {width: imageSize, height: imageSize}];
      return <Image source={asset.node.image} style={imageStyle} />;
    },
  };

  state = this.getInitialState();

  getInitialState() {
    return {
      assets: [],
      data: [],
      seen: new Set(),
      lastCursor: null,
      noMore: false,
      loadingMore: false,
    };
  }

  componentDidMount() {
    this.fetch();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (this.props.groupTypes !== nextProps.groupTypes) {
      this.fetch(true);
    }
  }

  async _fetch(clear) {
    if (clear) {
      this.setState(this.getInitialState(), this.fetch);
      return;
    }

    if (Platform.OS === 'android') {
      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
        {
          title: 'Permission Explanation',
          message: 'RNTester would like to access your pictures.',
        },
      );
      if (result !== 'granted') {
        Alert.alert('Access to pictures was denied.');
        return;
      }
    }

    const fetchParams = {
      first: this.props.batchSize,
      groupTypes: this.props.groupTypes,
      assetType: this.props.assetType,
    };
    if (Platform.OS === 'android') {
      // not supported in android
      delete fetchParams.groupTypes;
    }

    if (this.state.lastCursor) {
      fetchParams.after = this.state.lastCursor;
    }

    try {
      const data = await CameraRoll.getPhotos(fetchParams);
      this._appendAssets(data);
    } catch (e) {
      logError(e);
    }
  }

  /**
   * Fetches more images from the camera roll. If clear is set to true, it will
   * set the component to its initial state and re-fetch the images.
   */
  fetch = clear => {
    if (!this.state.loadingMore) {
      this.setState({loadingMore: true}, () => {
        this._fetch(clear);
      });
    }
  };

  render() {
    console.log({data: this.state.data});
    return (
      <FlatList
        keyExtractor={(_, idx) => String(idx)}
        renderItem={this._renderItem}
        ListFooterComponent={this._renderFooterSpinner}
        onEndReached={this._onEndReached}
        onEndReachedThreshold={0.2}
        style={styles.container}
        data={this.state.data || []}
        extraData={this.props.bigImages + this.state.noMore}
      />
    );
  }

  _renderFooterSpinner = () => {
    if (!this.state.noMore) {
      return <ActivityIndicator />;
    }
    return null;
  };

  _renderItem = ({item}) => {
    console.log({item});
    return (
      <View style={styles.row}>
        {item.map(image => (image ? this.props.renderImage(image) : null))}
      </View>
    );
  };

  _appendAssets(data) {
    const assets = data.edges;
    const newState = {loadingMore: false};

    if (!data.page_info.has_next_page) {
      newState.noMore = true;
    }

    if (assets.length > 0) {
      newState.lastCursor = data.page_info.end_cursor;
      newState.seen = new Set(this.state.seen);

      // Unique assets efficiently
      // Checks new pages against seen objects
      const uniqAssets = [];
      for (let index = 0; index < assets.length; index++) {
        const asset = assets[index];
        let value = asset.node.image.uri;
        if (newState.seen.has(value)) {
          continue;
        }
        newState.seen.add(value);
        uniqAssets.push(asset);
      }

      newState.assets = this.state.assets.concat(uniqAssets);
      newState.data = groupByEveryN(this.props.imagesPerRow)(newState.assets);
    }

    this.setState(newState);
  }

  _onEndReached = () => {
    if (!this.state.noMore) {
      this.fetch();
    }
  };
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    flex: 1,
  },
  image: {
    margin: 4,
  },
  container: {
    flex: 1,
  },
});

module.exports = CameraRollView;
