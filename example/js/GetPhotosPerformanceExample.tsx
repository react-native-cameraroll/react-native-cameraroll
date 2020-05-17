import * as React from 'react';
import {StyleSheet, View, Button, Text, TextInput} from 'react-native';
// @ts-ignore: CameraRollExample has no typings in same folder
import CameraRoll from '../../js/CameraRoll';

interface State {
  fetchingPhotos: boolean;
  timeTakenMillis: number | null;
  output: CameraRoll.PhotoIdentifiersPage | null;
}

enum FunctionName {
  getPhotos = 'getPhotos',
  getPhotosFast = 'getPhotosFast',
}

/**
 * Example for testing performance differences between `getPhotos` and
 * `getPhotosFast`
 */
export default class GetPhotosPerformanceExample extends React.PureComponent<
  {},
  State
> {
  state: State = {fetchingPhotos: false, timeTakenMillis: null, output: null};

  startFetchingPhotos = async (functionName: FunctionName) => {
    this.setState({fetchingPhotos: true});
    const getPhotosFn = CameraRoll[functionName];
    const params: CameraRoll.GetPhotosFastParams = {first: 1000};
    const startTime = Date.now();
    const output: CameraRoll.PhotoIdentifiersPage = await getPhotosFn(params);
    const endTime = Date.now();
    this.setState({
      output,
      timeTakenMillis: endTime - startTime,
      fetchingPhotos: false,
    });
  };

  render() {
    const {fetchingPhotos, timeTakenMillis, output} = this.state;
    return (
      <View style={styles.flex1}>
        {Object.values(FunctionName).map(functionName => (
          <Button
            key={functionName}
            disabled={fetchingPhotos}
            title={`Run ${functionName} on 1000 photos`}
            onPress={() => this.startFetchingPhotos(functionName)}
          />
        ))}
        {timeTakenMillis !== null && (
          <Text>Time taken: {timeTakenMillis} ms</Text>
        )}
        <View>
          <Text>Output</Text>
        </View>
        <TextInput
          style={styles.textInput}
          editable={false}
          multiline
          value={JSON.stringify(output, null, 2)}
        />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  flex1: {flex: 1, padding: 10},
  textInput: {
    flex: 1,
    borderColor: '#ccc',
    borderWidth: 1,
    padding: 10,
  },
});
