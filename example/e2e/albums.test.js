import {device, expect, element, by} from 'detox';

describe('CameraRoll', () => {
  beforeEach(async () => {
    await device.launchApp({
      permissions: {
        photos: 'YES',
        // camera: 'YES',
      },
    });
  });

  it('picks album and filters assets by it', async () => {
    await element(by.text('CHANGE EXAMPLE')).tap();
    await element(by.text('ALBUMSEXAMPLE')).tap();

    await element(by.id('select-album')).tap();
    await element(by.text('PICTURES (7)')).tap();

    await expect(element(by.id('Pictures/7.jpg'))).toExist();
    await expect(element(by.id('Pictures/6.jpg'))).toExist();
    await expect(element(by.id('Pictures/5.jpg'))).toExist();
    await expect(element(by.id('Pictures/4.jpg'))).toExist();
    await expect(element(by.id('Pictures/3.jpg'))).toExist();
    await expect(element(by.id('Pictures/2.jpg'))).toExist();
    await expect(element(by.id('Pictures/1.jpg'))).toExist();
    await expect(element(by.id('album-1/2.jpg'))).not.toExist();
    await expect(element(by.id('album-1/1.jpg'))).not.toExist();

    await element(by.id('select-album')).tap();
    await element(by.text('ALBUM-1 (2)')).tap();

    await expect(element(by.id('Pictures/7.jpg'))).not.toExist();
    await expect(element(by.id('Pictures/6.jpg'))).not.toExist();
    await expect(element(by.id('Pictures/5.jpg'))).not.toExist();
    await expect(element(by.id('Pictures/4.jpg'))).not.toExist();
    await expect(element(by.id('Pictures/3.jpg'))).not.toExist();
    await expect(element(by.id('Pictures/2.jpg'))).not.toExist();
    await expect(element(by.id('Pictures/1.jpg'))).not.toExist();
    await expect(element(by.id('album-1/2.jpg'))).toExist();
    await expect(element(by.id('album-1/1.jpg'))).toExist();
  });
});
