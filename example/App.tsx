import React, { useEffect, useState } from 'react';
import {
  View,
  FlatList,
  Image,
  Button,
  StyleSheet,
  PermissionsAndroid,
  Platform,
  Alert,
} from 'react-native';
import { CameraRoll, PhotoIdentifier } from '@react-native-camera-roll/camera-roll';

const App: React.FC = () => {
  const [photos, setPhotos] = useState<PhotoIdentifier[]>([]);
  const [selectedUris, setSelectedUris] = useState<string[]>([]);

  const hasAndroidPermission = async (): Promise<boolean> => {
    const getCheckPermissionPromise = async (): Promise<boolean> => {
      if (Number(Platform.Version) >= 33) {
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

    const getRequestPermissionPromise = (): Promise<boolean> => {
      if (Number(Platform.Version) >= 33) {
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
        return PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
        ).then((status) => status === PermissionsAndroid.RESULTS.GRANTED);
      }
    };

    return await getRequestPermissionPromise();
  };

  const fetchPhotos = async (): Promise<void> => {
    const permissionGranted = await hasAndroidPermission();
    if (permissionGranted) {
      const result = await CameraRoll.getPhotos({
        first: 10,
        assetType: 'All',
      });
      setPhotos(result.edges.map((edge) => edge as PhotoIdentifier));
    }
  };

  useEffect(() => {
    fetchPhotos();
  }, []);

  const toggleSelectPhoto = (uri: string): void => {
    setSelectedUris((prev) =>
      prev.includes(uri) ? prev.filter((item) => item !== uri) : [...prev, uri],
    );
  };

  const deletePhotos = async (): Promise<void> => {
    if (selectedUris.length === 0) {
      Alert.alert('No photos selected', 'Please select photos to delete.');
      return;
    }

    try {
      await CameraRoll.deletePhotos(selectedUris);
      Alert.alert('Success', 'Photos deleted successfully.');
      // Refresh the photo list
      setSelectedUris([]);
      fetchPhotos();
    } catch (error) {
      console.error('Error deleting photos: ', error);
      Alert.alert('Error', String(error));
    }
  };

  const renderItem = ({ item }: { item: PhotoIdentifier }): React.ReactElement => (
    <View style={styles.photoContainer}>
      <Image
        source={{ uri: item.node.image.uri }}
        style={styles.image}
      />
      <Button
        title={selectedUris.includes(item.node.image.uri) ? 'Deselect' : 'Select'}
        onPress={() => toggleSelectPhoto(item.node.image.uri)}
      />
    </View>
  );

  return (
    <View style={styles.container}>
      <Button title="Refresh Photos" onPress={fetchPhotos} />
      <Button title="Delete Selected Photos" onPress={deletePhotos} />
      <FlatList
        data={photos}
        renderItem={renderItem}
        keyExtractor={(item) => item.node.image.uri}
        numColumns={2}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 10,
    backgroundColor: '#fff',
  },
  photoContainer: {
    flex: 1,
    margin: '1%',
    alignItems: 'center',
  },
  image: {
    width: '100%',
    height: 150,
  },
});

export default App;