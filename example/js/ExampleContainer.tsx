import * as React from 'react';
import {
  SafeAreaView,
  StyleSheet,
  View,
  Button,
  Modal,
  TouchableWithoutFeedback,
} from 'react-native';
// @ts-ignore: CameraRollExample has no typings in same folder
import CameraRollExample from './CameraRollExample';
import GetPhotosPerformanceExample from './GetPhotosPerformanceExample';

interface Props {}

interface State {
  showChangeExampleModal: boolean;
  currentExampleIndex: number;
}

interface Example {
  label: string;
  Component: React.ComponentType;
}

const examples: Example[] = [
  {
    label: 'CameraRollExample',
    Component: CameraRollExample,
  },
  {
    label: 'GetPhotosPerformanceExample',
    Component: GetPhotosPerformanceExample,
  },
];

/**
 * Container for displaying and switching between multiple examples.
 *
 * Shows a button which opens up a Modal to switch between examples, as well
 * as the current example itself.
 */
export default class ExamplesContainer extends React.Component<Props, State> {
  state: State = {showChangeExampleModal: false, currentExampleIndex: 0};

  render() {
    const {currentExampleIndex} = this.state;
    return (
      <SafeAreaView style={styles.flex1}>
        <Button
          title="Change example"
          onPress={() => this.setState({showChangeExampleModal: true})}
        />
        {this._renderChangeExampleModal()}
        <View style={styles.flex1}>
          {React.createElement(examples[currentExampleIndex].Component)}
        </View>
      </SafeAreaView>
    );
  }

  _renderChangeExampleModal() {
    const {showChangeExampleModal} = this.state;
    return (
      <Modal visible={showChangeExampleModal} transparent>
        <TouchableWithoutFeedback
          onPress={() => this.setState({showChangeExampleModal: false})}>
          <View style={styles.modalScrim}>
            <SafeAreaView>
              <View style={styles.modalInner}>
                {examples.map((example, index) => (
                  <Button
                    key={example.label}
                    title={example.label}
                    onPress={() =>
                      this.setState({
                        currentExampleIndex: index,
                        showChangeExampleModal: false,
                      })
                    }
                  />
                ))}
              </View>
            </SafeAreaView>
          </View>
        </TouchableWithoutFeedback>
      </Modal>
    );
  }
}

const styles = StyleSheet.create({
  modalScrim: {
    flex: 1,
    backgroundColor: '#00000080',
  },
  flex1: {
    flex: 1,
  },
  modalInner: {
    margin: 20,
    backgroundColor: '#fff',
  },
});
