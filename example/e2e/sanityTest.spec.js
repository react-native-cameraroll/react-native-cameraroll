const {device, expect, element, by, waitFor} = require('detox');

describe('CameraRoll', () => {
  beforeEach(async () => {
    await device.launchApp({permissions: {
        photos: 'YES',
        // camera: 'YES',
    }});
  });

  it('should load example app with no errors and show all the examples by default', async () => {
    await expect(element(by.text('Big Images'))).toExist();
  });

});
