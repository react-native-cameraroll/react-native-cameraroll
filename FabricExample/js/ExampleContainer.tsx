import React from 'react';
import {Component} from 'react';
import {
  SafeAreaView,
  StyleSheet,
  View,
  Button,
  Modal,
  TouchableWithoutFeedback,
  Appearance,
} from 'react-native';
// @ts-ignore: CameraRollExample has no typings in same folder
import CameraRollExample from './CameraRollExample';
import GetPhotosPerformanceExample from './GetPhotosPerformanceExample';
import GetAlbumsExample from './GetAlbumsExample';

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
    label: 'GetPhotosPerformanceExample',
    Component: GetPhotosPerformanceExample,
  },
  {
    label: 'GetAlbumsExample',
    Component: GetAlbumsExample,
  },
  {
    label: 'CameraRollExample',
    Component: CameraRollExample,
  },
];

/**
 * Container for displaying and switching between multiple examples.
 *
 * Shows a button which opens up a Modal to switch between examples, as well
 * as the current example itself.
 */
export default class ExamplesContainer extends Component<Props, State> {
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
    backgroundColor:
      Appearance.getColorScheme() === 'light' ? '#00000080' : '#ffffff80',
  },
  flex1: {
    flex: 1,
  },
  modalInner: {
    margin: 20,
    backgroundColor: Appearance.getColorScheme() === 'light' ? '#fff' : '#000',
  },
});
