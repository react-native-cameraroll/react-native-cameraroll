/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow
 * @format
 */

import CameraRoll from '../CameraRoll';

const NativeModule = require('../nativeInterface');

jest.mock('../nativeInterface');

describe('CameraRoll', () => {
  it('Should call deletePhotos', () => {
    CameraRoll.deletePhotos(['a uri']);
    expect(NativeModule.deletePhotos.mock.calls).toMatchSnapshot();
  });

  it('Should call saveToCameraRoll', async () => {
    await CameraRoll.saveToCameraRoll('a tag', 'photo');
    expect(NativeModule.saveToCameraRoll.mock.calls).toMatchSnapshot();
  });

  it('Should call save', async () => {
    await CameraRoll.save('a tag', {type: 'photo'});
    expect(NativeModule.saveToCameraRoll.mock.calls).toMatchSnapshot();
  });

  it('Should call getPhotos', async () => {
    await CameraRoll.getPhotos({first: 0});
    expect(NativeModule.getPhotos.mock.calls).toMatchSnapshot();
  });
});
