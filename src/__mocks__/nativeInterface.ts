/* eslint-env jest */

module.exports = {
  deletePhotos: jest.fn(),
  saveToCameraRoll: jest.fn().mockResolvedValue({node: {image: {uri: ''}}}),
  getPhotos: jest.fn(),
};
