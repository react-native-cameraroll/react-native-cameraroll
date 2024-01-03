import {CameraRoll} from '../CameraRoll';

import NativeModule from '../NativeCameraRollModule';

let mockDeletePhotos: jest.Mock;
let mockSaveToCameraRoll: jest.Mock;
let mockGetPhotos: jest.Mock;

jest.mock('../NativeCameraRollModule', () => {
  mockDeletePhotos = jest.fn();
  mockSaveToCameraRoll = jest.fn(() =>
    Promise.resolve({node: {image: {uri: ''}}}),
  );
  mockGetPhotos = jest.fn();
  return {
    deletePhotos: mockDeletePhotos,
    saveToCameraRoll: mockSaveToCameraRoll,
    getPhotos: mockGetPhotos,
  };
});

describe('CameraRoll', () => {
  it('Should call deletePhotos', async () => {
    await CameraRoll.deletePhotos(['a uri']);
    expect(
      (NativeModule.deletePhotos as jest.Mock).mock.calls,
    ).toMatchSnapshot();
  });

  it('Should call saveToCameraRoll', async () => {
    await CameraRoll.saveToCameraRoll('a tag', 'photo');
    expect(
      (NativeModule.saveToCameraRoll as jest.Mock).mock.calls,
    ).toMatchSnapshot();
  });

  it('Should call getPhotos', async () => {
    await CameraRoll.getPhotos({first: 0});
    expect((NativeModule.getPhotos as jest.Mock).mock.calls).toMatchSnapshot();
  });
});
