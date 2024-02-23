import {CameraRoll} from '@react-native-camera-roll/camera-roll';
import * as React from 'react';
import {
  Appearance,
  Button,
  Keyboard,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from 'react-native';
import type {
  GetPhotosParams,
  Include,
  PhotoIdentifiersPage,
} from '@react-native-camera-roll/camera-roll';

interface State {
  colorScheme: 'light' | 'dark';
  fetchingPhotos: boolean;
  timeTakenMillis: number | null;
  output: PhotoIdentifiersPage | null;
  include: Include[];
  /**
   * `first` argument passed into `getPhotos`, but as a string. Validate it
   * with `this.first()` before using.
   */
  firstStr: string;
  includeSharedAlbums: boolean;
}

const includeValues: Include[] = [
  'filename',
  'fileSize',
  'fileExtension',
  'location',
  'imageSize',
  'playableDuration',
  'orientation',
  'albums',
  'sourceType',
];

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
    fetchingPhotos: false,
    timeTakenMillis: null,
    output: null,
    include: [],
    firstStr: '1000',
    includeSharedAlbums: false,
  };

  first = () => {
    const first = parseInt(this.state.firstStr, 10);
    if (first < 0 || !Number.isInteger(first)) {
      return null;
    }
    return first;
  };

  startFetchingPhotos = async () => {
    const {include, includeSharedAlbums} = this.state;
    const first = this.first();
    if (first === null) {
      return;
    }
    this.setState({fetchingPhotos: true});
    Keyboard.dismiss();
    const params: GetPhotosParams = {
      first,
      include,
      includeSharedAlbums,
    };
    const startTime = Date.now();
    const output: PhotoIdentifiersPage = await CameraRoll.getPhotos(params);
    const endTime = Date.now();
    this.setState({
      output,
      timeTakenMillis: endTime - startTime,
      fetchingPhotos: false,
    });
  };

  handleIncludeChange = (includeValue: Include, changedTo: boolean) => {
    if (changedTo === false) {
      const include = this.state.include.filter(
        value => value !== includeValue,
      );
      this.setState({include});
    } else {
      const include = [...this.state.include, includeValue];
      this.setState({include});
    }
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
    const {
      fetchingPhotos,
      timeTakenMillis,
      output,
      include,
      firstStr,
      includeSharedAlbums,
    } = this.state;
    const first = this.first();

    return (
      <View style={[styles.container, this.styles.container]}>
        {includeValues.map(includeValue => (
          <View
            key={includeValue + this.state.colorScheme}
            style={styles.inputRow}>
            <Text style={this.styles.text}>{includeValue}</Text>
            <Switch
              value={include.includes(includeValue)}
              onValueChange={(changedTo: boolean) =>
                this.handleIncludeChange(includeValue, changedTo)
              }
            />
          </View>
        ))}
        <View key="includeSharedAlbums" style={styles.inputRow}>
          <Text style={this.styles.text}>includeSharedAlbums</Text>
          <Switch
            value={includeSharedAlbums}
            onValueChange={(changedTo: boolean) =>
              this.setState({includeSharedAlbums: changedTo})
            }
          />
        </View>
        <View style={styles.inputRow}>
          <Text style={this.styles.text}>
            first
            {first === null && (
              <Text style={styles.error}> (enter a positive number)</Text>
            )}
          </Text>
          <TextInput
            value={firstStr}
            onChangeText={(text: string) => this.setState({firstStr: text})}
            style={[
              styles.textInput,
              this.styles.text,
              first === null && styles.textInputError,
            ]}
          />
        </View>
        <Button
          disabled={fetchingPhotos}
          title={`Run getPhotos on ${first} photos`}
          onPress={this.startFetchingPhotos}
        />
        {timeTakenMillis !== null && (
          <Text style={this.styles.text}>Time taken: {timeTakenMillis} ms</Text>
        )}
        <View>
          <Text style={this.styles.text}>
            Output : {output?.edges?.length || '0'} elements
          </Text>
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
