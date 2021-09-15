import React, {
  FunctionComponent,
  useCallback,
  useEffect,
  useState,
} from 'react';
import {
  Modal,
  SafeAreaView,
  TouchableWithoutFeedback,
  View,
  StyleSheet,
  Button,
  Text,
} from 'react-native';

import CameraRoll from '../../js/CameraRoll';
import {Album, PhotoIdentifier} from '../../typings/CameraRoll';
import CameraRollView from './CameraRollView';

const INCLUDE = ['filename'];

export const AlbumsExample: FunctionComponent = () => {
  const [albums, setAlbums] = useState<Album[]>([]);
  const [selectedAlbum, setSelectedAlbum] = useState<string | undefined>(
    undefined,
  );
  const [pickAlbumVisible, setPickAlbumVisible] = useState(false);

  const closePicker = useCallback(() => {
    setPickAlbumVisible(false);
  }, []);

  const openPicker = useCallback(() => {
    setPickAlbumVisible(true);
  }, []);

  const pickAlbum = useCallback(
    (album: Album) => {
      setSelectedAlbum(album.title);
      closePicker();
    },
    [closePicker],
  );

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

  useEffect(() => {
    CameraRoll.getAlbums().then(setAlbums);
  }, []);

  return (
    <>
      <View style={styles.pickAlbumContainer}>
        <Text>
          Selected album:{' '}
          <Text style={styles.bold}>{selectedAlbum || 'None'}</Text>
        </Text>
        <Button testID="select-album" title="Select" onPress={openPicker} />
      </View>
      {selectedAlbum ? (
        <CameraRollView
          batchSize={10}
          include={INCLUDE}
          renderImage={renderImage}
          flatList={false}
          groupName={selectedAlbum}
        />
      ) : (
        undefined
      )}
      <Modal visible={pickAlbumVisible} transparent>
        <TouchableWithoutFeedback onPress={closePicker}>
          <View style={styles.modalScrim}>
            <SafeAreaView>
              <View style={styles.modalInner}>
                {albums.map(album => (
                  <Button
                    key={album.title}
                    title={`${album.title} (${album.count})`}
                    onPress={pickAlbum.bind(null, album)}
                  />
                ))}
              </View>
            </SafeAreaView>
          </View>
        </TouchableWithoutFeedback>
      </Modal>
    </>
  );
};

const styles = StyleSheet.create({
  pickAlbumContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 20,
  },
  bold: {
    fontWeight: 'bold',
  },
  modalScrim: {
    flex: 1,
    backgroundColor: '#00000080',
  },
  modalInner: {
    margin: 20,
    backgroundColor: '#fff',
  },
  imageContainer: {
    padding: 10,
  },
});
