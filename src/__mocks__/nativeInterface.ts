/* eslint-env jest */

module.exports = {
  deletePhotos: jest.fn(),
  saveToCameraRoll: () => Promise.resolve(({ node: { image: { uri: '' } } })),
  getPhotos: jest.fn(),
};
