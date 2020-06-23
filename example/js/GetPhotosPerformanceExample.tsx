import * as React from 'react';
import {
  StyleSheet,
  View,
  Button,
  Text,
  Switch,
  TextInput,
  Keyboard,
} from 'react-native';
// @ts-ignore: CameraRollExample has no typings in same folder
import CameraRoll from '../../js/CameraRoll';

interface State {
  fetchingPhotos: boolean;
  timeTakenMillis: number | null;
  output: CameraRoll.PhotoIdentifiersPage | null;
  include: CameraRoll.Include[];
  /**
   * `first` argument passed into `getPhotos`, but as a string. Validate it
   * with `this.first()` before using.
   */
  firstStr: string;
}

const includeValues: CameraRoll.Include[] = [
  'filename',
  'fileSize',
  'location',
  'imageSize',
  'playableDuration',
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
    fetchingPhotos: false,
    timeTakenMillis: null,
    output: null,
    include: [],
    firstStr: '1000',
  };

  first = () => {
    const first = parseInt(this.state.firstStr, 10);
    if (first < 0 || !Number.isInteger(first)) {
      return null;
    }
    return first;
  };

  startFetchingPhotos = async () => {
    const {include} = this.state;
    const first = this.first();
    if (first === null) {
      return;
    }
    this.setState({fetchingPhotos: true});
    Keyboard.dismiss();
    const params: CameraRoll.GetPhotosParams = {first, include};
    const startTime = Date.now();
    const output: CameraRoll.PhotoIdentifiersPage = await CameraRoll.getPhotos(
      params,
    );
    const endTime = Date.now();
    this.setState({
      output,
      timeTakenMillis: endTime - startTime,
      fetchingPhotos: false,
    });
  };

  handleIncludeChange = (
    includeValue: CameraRoll.Include,
    changedTo: boolean,
  ) => {
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

  render() {
    const {
      fetchingPhotos,
      timeTakenMillis,
      output,
      include,
      firstStr,
    } = this.state;
    const first = this.first();

    return (
      <View style={styles.container}>
        {includeValues.map(includeValue => (
          <View key={includeValue} style={styles.inputRow}>
            <Text>{includeValue}</Text>
            <Switch
              value={include.includes(includeValue)}
              onValueChange={(changedTo: boolean) =>
                this.handleIncludeChange(includeValue, changedTo)
              }
            />
          </View>
        ))}
        <View style={styles.inputRow}>
          <Text>
            first
            {first === null && (
              <Text style={styles.error}> (enter a positive number)</Text>
            )}
          </Text>
          <TextInput
            value={firstStr}
            onChangeText={(text: string) => this.setState({firstStr: text})}
            style={[styles.textInput, first === null && styles.textInputError]}
          />
        </View>
        <Button
          disabled={fetchingPhotos}
          title={`Run getPhotos on ${first} photos`}
          onPress={this.startFetchingPhotos}
        />
        {timeTakenMillis !== null && (
          <Text>Time taken: {timeTakenMillis} ms</Text>
        )}
        <View>
          <Text>Output</Text>
        </View>
        <TextInput
          value={JSON.stringify(output, null, 2)}
          multiline
          style={styles.outputBox}
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
