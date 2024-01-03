/* eslint-env jest */

module.exports = {
  deletePhotos: jest.fn(),
  saveToCameraRoll: jest.fn(() => Promise.resolve({node: {image: {uri: ''}}})),
  getPhotos: jest.fn(),
};
