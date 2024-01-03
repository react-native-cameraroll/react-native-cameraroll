import {renderHook, waitFor} from '@testing-library/react-native';
import {useCameraRoll} from '../useCameraRoll';
import RNCCameraRoll from '../NativeCameraRollModule';

jest.mock('../NativeCameraRollModule', () => ({
  getPhotos: jest.fn(),
  saveToCameraRoll: jest.fn(() => Promise.resolve({node: {image: {uri: ''}}})),
}));

describe('useCameraRoll()', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should return initial photos by default', () => {
    const {result} = renderHook(() => useCameraRoll());

    expect(result.current).toEqual([
      {
        edges: [],
        page_info: {end_cursor: '', has_next_page: false, start_cursor: ''},
      },
      expect.any(Function),
      expect.any(Function),
    ]);
  });

  describe('saveToCameraRoll()', () => {
    it('should invoke save with passed params', async () => {
      const tag = 'mock-tag';
      const type = 'video';
      const album = 'test-album';
      const {result} = renderHook(useCameraRoll);
      const [, , saveToCameraRoll] = result.current;

      (RNCCameraRoll.saveToCameraRoll as jest.Mock).mockResolvedValue({
        node: {image: {uri: ''}},
      });

      await saveToCameraRoll(tag, {type, album});

      expect(RNCCameraRoll.saveToCameraRoll).toHaveBeenCalledWith(tag, {
        album,
        type,
      });
    });
  });

  describe('getPhotos()', () => {
    // eslint-disable-next-line @typescript-eslint/explicit-function-return-type
    const createPhotosMock = ({
      edges = [] as Array<{node: {type: string}}>,
      has_next_page = false,
      start_cursor = '',
      end_cursor = '',
      limited = false,
    } = {}) => ({
      edges,
      limited,
      page_info: {has_next_page, start_cursor, end_cursor},
    });

    it('should invoke getPhotos with default params', async () => {
      const {result} = renderHook(useCameraRoll);
      const [, getPhotos] = result.current;

      (RNCCameraRoll.getPhotos as jest.Mock).mockResolvedValueOnce(
        createPhotosMock(),
      );

      await getPhotos();

      expect(RNCCameraRoll.getPhotos).toHaveBeenCalledWith({
        assetType: 'All',
        first: 20,
        groupTypes: 'All',
      });
    });

    it('should invoke getPhotos with custom params', async () => {
      const customParams = {
        first: 1,
        assetType: 'Photos' as const,
        include: ['filename' as const],
      };
      (RNCCameraRoll.getPhotos as jest.Mock).mockResolvedValueOnce(
        createPhotosMock(),
      );
      const {result} = renderHook(useCameraRoll);
      const [, getPhotos] = result.current;

      await getPhotos(customParams);

      expect(RNCCameraRoll.getPhotos).toHaveBeenCalledWith({
        assetType: 'Photos',
        first: 1,
        groupTypes: 'All',
        include: ['filename'],
      });
    });

    it('should return result of getPhotos', async () => {
      const mockPhotos = createPhotosMock({
        edges: [{node: {type: 'mock-type'}}],
      });
      (RNCCameraRoll.getPhotos as jest.Mock).mockResolvedValueOnce(mockPhotos);
      const {result, rerender} = renderHook(useCameraRoll);
      const [, getPhotos] = result.current;

      await getPhotos();

      rerender({});
      const [photos] = result.current;

      await waitFor(() => expect(photos).toEqual(mockPhotos));
    });

    it('should handle an error when invoke getPhotos', async () => {
      const error = new Error('Ops...');
      (RNCCameraRoll.getPhotos as jest.Mock).mockRejectedValueOnce(error);
      const {result} = renderHook(useCameraRoll);
      const [initialPhotos, getPhotos] = result.current;

      await getPhotos();

      const [afterError] = result.current;

      expect(initialPhotos).toBe(afterError);
    });
  });
});
