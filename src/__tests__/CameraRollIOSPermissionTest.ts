let mockCheckPermission: jest.Mock;
let mockRequestReadWritePermission: jest.Mock;
let mockRequestAddOnlyPermission: jest.Mock;
let mockRefreshPhotoSelection: jest.Mock;

jest.mock('../NativeCameraRollPermissionModule', () => {
  mockCheckPermission = jest.fn();
  mockRequestReadWritePermission = jest.fn();
  mockRequestAddOnlyPermission = jest.fn();
  mockRefreshPhotoSelection = jest.fn();
  return {
    checkPermission: mockCheckPermission,
    requestReadWritePermission: mockRequestReadWritePermission,
    requestAddOnlyPermission: mockRequestAddOnlyPermission,
    refreshPhotoSelection: mockRefreshPhotoSelection,
  };
});

jest.mock('react-native', () => {
  return {
    NativeEventEmitter: jest.fn().mockImplementation(() => {
      return {
        removeAllListeners: () => {
          console.log;
        },
      };
    }),
    Platform: {
      OS: 'ios',
    },
  };
});

import {
  iosReadGalleryPermission,
  iosRequestReadWriteGalleryPermission,
  iosRequestAddOnlyGalleryPermission,
  iosRefreshGallerySelection,
} from '../CameraRollIOSPermission';

describe('CameraRollIosPermission', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('Should call checkPermission if iosReadGalleryPermission is called', () => {
    iosReadGalleryPermission('addOnly');
    expect(mockCheckPermission).toHaveBeenCalledTimes(1);
    expect(mockCheckPermission).toHaveBeenCalledWith('addOnly');
  });

  it('Should call requestReadWritePermission if iosRequestReadWriteGalleryPermission is called', () => {
    iosRequestReadWriteGalleryPermission();
    expect(mockRequestReadWritePermission).toHaveBeenCalledTimes(1);
  });

  it('Should call requestAddOnlyPermission if iosRequestAddOnlyGalleryPermission is called', () => {
    iosRequestAddOnlyGalleryPermission();
    expect(mockRequestAddOnlyPermission).toHaveBeenCalledTimes(1);
  });

  it('Should call refreshPhotoSelection if iosRefreshGallerySelection is called', () => {
    iosRefreshGallerySelection();
    expect(mockRefreshPhotoSelection).toHaveBeenCalledTimes(1);
  });
});
