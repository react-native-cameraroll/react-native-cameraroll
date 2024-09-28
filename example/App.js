import React, { useEffect, useState } from 'react';
import { View, FlatList, Image, Button, StyleSheet, PermissionsAndroid, Platform, Alert } from 'react-native';
import { CameraRoll } from '@react-native-camera-roll/camera-roll';

const App = () => {
  const [photos, setPhotos] = useState([]);
  const [selectedUris, setSelectedUris] = useState([]);
console.log(selectedUris, "selected");

  async function hasAndroidPermission() {
  const getCheckPermissionPromise = () => {
    if (Platform.Version >= 33) {
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
  const getRequestPermissionPromise = () => {
    if (Platform.Version >= 33) {
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
      return PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE).then((status) => status === PermissionsAndroid.RESULTS.GRANTED);
    }
  };

  return await getRequestPermissionPromise();
}

  const fetchPhotos = async () => {
    const permissionGranted = await hasAndroidPermission();
    if (permissionGranted) {
      const result = await CameraRoll.getPhotos({
        first: 10,
        assetType: 'All',
      });
      setPhotos(result.edges);
    }
  };

  useEffect(() => {
    fetchPhotos();
  }, []);

  const toggleSelectPhoto = (uri) => {
    setSelectedUris((prev) => {
      if (prev.includes(uri)) {
        return prev.filter(item => item !== uri);
      } else {
        return [...prev, uri];
      }
    });
  };

  const deletePhotos = async () => {
    if (selectedUris.length === 0) {
      Alert.alert("No photos selected", "Please select photos to delete.");
      return;
    }
    
    try {
      await CameraRoll.deletePhotos(selectedUris);
      Alert.alert("Success", "Photos deleted successfully.");
      // Refresh the photo list
      setSelectedUris([]);
      fetchPhotos();
    } catch (error) {
      console.error("Error deleting photos: ", error);
      Alert.alert("Error", String(error));
    }
  };

  const renderItem = ({ item }) => (
    <View style={styles.photoContainer}>
      <Image
        source={{ uri: item.node.image.uri.replace("file", "images/media") }}
        style={styles.image}
      />
      <Button
        title={selectedUris.includes(item.node.image.uri) ? "Deselect" : "Select"}
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
    width: '100%', // Adjusts the width for 2 columns
    height: 150,
  },
});

export default App;