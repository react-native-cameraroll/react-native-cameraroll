import * as React from 'react';
import {StyleSheet, View, Button, Text, TextInput, Switch} from 'react-native';
// @ts-ignore: CameraRollExample has no typings in same folder
import CameraRoll from '../../js/CameraRoll';

interface State {
  fetchingPhotos: boolean;
  timeTakenMillis: number | null;
  output: CameraRoll.PhotoIdentifiersPage | null;
  include: CameraRoll.Include[];
}

const includeValues: CameraRoll.Include[] = [
  'filename',
  'fileSize',
  'location',
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
  };

  startFetchingPhotos = async () => {
    const {include} = this.state;
    this.setState({fetchingPhotos: true});
    const params: CameraRoll.GetPhotosParams = {first: 1000, include};
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
    const {fetchingPhotos, timeTakenMillis, output, include} = this.state;
    return (
      <View style={styles.flex1}>
        {includeValues.map(includeValue => (
          <View key={includeValue} style={styles.switchRow}>
            <Text>{includeValue}</Text>
            <Switch
              value={include.includes(includeValue)}
              onValueChange={(changedTo: boolean) =>
                this.handleIncludeChange(includeValue, changedTo)
              }
            />
          </View>
        ))}
        <Button
          disabled={fetchingPhotos}
          title="Run getPhotos on 1000 photos"
          onPress={this.startFetchingPhotos}
        />
        {timeTakenMillis !== null && (
          <Text>Time taken: {timeTakenMillis} ms</Text>
        )}
        <View>
          <Text>Output</Text>
        </View>
        <TextInput
          style={styles.textInput}
          multiline
          value={JSON.stringify(output, null, 2)}
        />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  flex1: {flex: 1, padding: 10},
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  textInput: {
    flex: 1,
    borderColor: '#ccc',
    borderWidth: 1,
    padding: 10,
  },
});
