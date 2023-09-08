import * as React from 'react';
import {
  StyleSheet,
  View,
  Button,
  Text,
  TextInput,
  Keyboard,
  Appearance,
} from 'react-native';
import {CameraRoll} from '@react-native-camera-roll/camera-roll';
import type {
  // GetAlbumsParams,
  Album,
} from '@react-native-camera-roll/camera-roll';

interface State {
  colorScheme: 'light' | 'dark';
  fetchingAlbums: boolean;
  timeTakenMillis: number | null;
  output: Album[] | null;
}

/**
 * Example for testing performance differences between `getPhotos` and
 * `getPhotosFast`
 */
export default class GetPhotosPerformanceExample extends React.PureComponent<
  {},
  State
> {
  state: State = {
    colorScheme: Appearance.getColorScheme() === 'light' ? 'light' : 'dark',
    fetchingAlbums: false,
    timeTakenMillis: null,
    output: null,
  };

  startFetchingAlbums = async () => {
    this.setState({fetchingAlbums: true});
    Keyboard.dismiss();
    // const params: GetAlbumsParams = {assetType: 'All'};
    const startTime = Date.now();
    const output: Album[] = await CameraRoll.getAlbums({assetType: 'All'});
    const endTime = Date.now();
    this.setState({
      output,
      timeTakenMillis: endTime - startTime,
      fetchingAlbums: false,
    });
  };

  componentDidMount(): void {
    Appearance.addChangeListener(({colorScheme}) => {
      this.setState({colorScheme: colorScheme === 'light' ? 'light' : 'dark'});
    });
  }

  get styles() {
    return {
      container: {
        backgroundColor: this.state.colorScheme === 'light' ? '#fff' : '#000',
      },
      text: {
        color: this.state.colorScheme === 'light' ? '#000' : '#fff',
      },
    };
  }

  render() {
    const {fetchingAlbums, timeTakenMillis, output} = this.state;

    return (
      <View style={[styles.container, this.styles.container]}>
        <Button
          disabled={fetchingAlbums}
          title={'Run getAlbums'}
          onPress={this.startFetchingAlbums}
        />
        {timeTakenMillis !== null && (
          <Text style={this.styles.text}>Time taken: {timeTakenMillis} ms</Text>
        )}
        <View>
          <Text style={this.styles.text}>Output</Text>
        </View>
        <TextInput
          value={JSON.stringify(output, null, 2)}
          multiline
          editable={false}
          style={[styles.outputBox, this.styles.text]}
        />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 8},
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 2,
  },
  textInput: {
    borderColor: '#ccc',
    borderWidth: 1,
    paddingVertical: 4,
    paddingHorizontal: 8,
    width: 150,
  },
  error: {color: '#f00'},
  textInputError: {borderColor: '#f00'},
  outputBox: {
    flex: 1,
    borderColor: '#ccc',
    borderWidth: 1,
    padding: 8,
  },
});
