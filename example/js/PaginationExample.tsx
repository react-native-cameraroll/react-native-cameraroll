import React, {FunctionComponent, useCallback, useRef} from 'react';
import {View, Text, StyleSheet, ViewStyle, Button} from 'react-native';
import {PhotoIdentifier} from '../../typings/CameraRoll';
import CameraRollView from './CameraRollView';

const INCLUDE = ['filename'];

export const PaginationExample: FunctionComponent = () => {
  const cameraRollView = useRef();
  const renderImage = useCallback((image: PhotoIdentifier) => {
    if (!image || !image.node) {
      return;
    }

    return (
      <View
        style={styles.container}
        testID={`${image.node.group_name}/${image.node.image.filename}`}>
        <Text>Group: {image.node.group_name}</Text>
        <Text>File name: {image.node.image.filename}</Text>
      </View>
    );
  }, []);

  const loadMore = useCallback(() => {
    cameraRollView.current.fetch();
  }, []);

  return (
    <>
      <CameraRollView
        ref={cameraRollView}
        batchSize={2}
        include={INCLUDE}
        renderImage={renderImage}
        flatList={false}
      />
      <Button title="LOAD MORE" onPress={loadMore} testID="load-more" />
    </>
  );
};

const styles = StyleSheet.create<{
  container: ViewStyle;
}>({
  container: {
    padding: 10,
  },
});
