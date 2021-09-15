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

  it('loads photos', async () => {
    await expect(element(by.text('Big Images'))).toExist();
    await expect(element(by.id('Pictures/7.jpg'))).toExist();
    await expect(element(by.id('Pictures/6.jpg'))).toExist();
    await expect(element(by.id('Pictures/5.jpg'))).toExist();
    await expect(element(by.id('Pictures/4.jpg'))).toExist();
  });

  it('paginates', async () => {
    await element(by.text('CHANGE EXAMPLE')).tap();
    await element(by.text('PAGINATIONEXAMPLE')).tap();

    await expect(element(by.id('Pictures/7.jpg'))).toExist();
    await expect(element(by.id('Pictures/6.jpg'))).toExist();
    await expect(element(by.id('Pictures/5.jpg'))).not.toExist();

    await element(by.id('load-more')).tap();
    await expect(element(by.id('Pictures/5.jpg'))).toExist();
    await expect(element(by.id('Pictures/4.jpg'))).toExist();
    await expect(element(by.id('Pictures/3.jpg'))).not.toExist();

    await element(by.id('load-more')).tap();
    await expect(element(by.id('Pictures/3.jpg'))).toExist();
    await expect(element(by.id('Pictures/2.jpg'))).toExist();
    await expect(element(by.id('Pictures/1.jpg'))).not.toExist();

    await element(by.id('load-more')).tap();
    await expect(element(by.id('Pictures/1.jpg'))).toExist();
    await expect(element(by.id('album-1/2.jpg'))).toExist();
    await expect(element(by.id('album-1/1.jpg'))).not.toExist();

    await element(by.id('load-more')).tap();

    await expect(element(by.id('Pictures/7.jpg'))).toExist();
    await expect(element(by.id('Pictures/6.jpg'))).toExist();
    await expect(element(by.id('Pictures/5.jpg'))).toExist();
    await expect(element(by.id('Pictures/4.jpg'))).toExist();
    await expect(element(by.id('Pictures/3.jpg'))).toExist();
    await expect(element(by.id('Pictures/2.jpg'))).toExist();
    await expect(element(by.id('Pictures/1.jpg'))).toExist();
    await expect(element(by.id('album-1/2.jpg'))).toExist();
    await expect(element(by.id('album-1/1.jpg'))).toExist();
  });
});
