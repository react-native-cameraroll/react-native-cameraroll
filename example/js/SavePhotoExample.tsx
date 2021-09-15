import React, {FunctionComponent, useCallback, useRef, useState} from 'react';
import {Button, View, Text, StyleSheet, Platform} from 'react-native';

import CameraRoll from '../../js/CameraRoll';
import {PhotoIdentifier} from '../../typings/CameraRoll';
import CameraRollView from './CameraRollView';

const INCLUDE = ['filename'];

export const SavePhotoExample: FunctionComponent = () => {
  const cameraRoll = useRef();
  const [savedPhotoUrl, setSavedPhotoUrl] = useState<string | undefined | null>(
    undefined,
  );
  const savePhoto = useCallback(async () => {
    setSavedPhotoUrl(undefined);
    const photo: PhotoIdentifier = (await CameraRoll.getPhotos({first: 1}))
      .edges[0];
    CameraRoll.save(photo.node.image.uri).then(setSavedPhotoUrl);
  }, []);

  const deletePhoto = useCallback(() => {
    setSavedPhotoUrl(undefined);
    CameraRoll.deletePhotos([
      Platform.OS === 'android'
        ? 'file:///storage/emulated/0/DCIM/7.jpg'
        : savedPhotoUrl,
    ]).then(() => {
      setSavedPhotoUrl(null);
    });
  }, [savedPhotoUrl]);

  const renderImage = useCallback((image: PhotoIdentifier) => {
    if (!image || !image.node) {
      return;
    }

    return (
      <View
        style={styles.imageContainer}
        testID={`${image.node.group_name}/${image.node.image.filename}`}>
        <Text>Group: {image.node.group_name}</Text>
        <Text>File name: {image.node.image.filename}</Text>
      </View>
    );
  }, []);

  return (
    <>
      <View style={styles.buttonsContainer}>
        <Button title="Save Photo" testID="save-photo" onPress={savePhoto} />
        <Button
          title="Delete Last Saved Photo"
          testID="delete-photo"
          onPress={deletePhoto}
          disabled={!savedPhotoUrl}
        />
      </View>
      {savedPhotoUrl !== undefined ? (
        <CameraRollView
          ref={cameraRoll}
          batchSize={10}
          include={INCLUDE}
          renderImage={renderImage}
          flatList={false}
        />
      ) : (
        undefined
      )}
    </>
  );
};

const styles = StyleSheet.create({
  imageContainer: {
    padding: 10,
  },
  buttonsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
});
