import {device, expect, element, by} from 'detox';
import { afterAll } from 'detox/runners/jest/adapter';

const child_process = require('child_process');
const {promisify} = require('util');

const exec = promisify(child_process.exec);

describe('CameraRoll', () => {
  beforeEach(async () => {
    await device.launchApp({
      permissions: {
        photos: 'YES',
        // camera: 'YES',
      },
    });
  });

  afterAll(async () => {
    // Clean up DCIM in Android, the default place where photos will be saved
    if (device.getPlatform() === 'android') {
      await exec("adb shell 'rm /sdcard/DCIM/*'");
    }
  });

  it('saves a photo, lists it, delets it and no longer lists it', async () => {
    await element(by.text('CHANGE EXAMPLE')).tap();
    await element(by.text('SAVEPHOTOEXAMPLE')).tap();

    await element(by.id('save-photo')).tap();

    let expectedId;
    if (device.getPlatform() === 'android') {
      expectedId = 'DCIM/7.jpg';
    }
    await expect(element(by.id(expectedId))).toExist();

    await element(by.id('delete-photo')).tap();
    await expect(element(by.id(expectedId))).not.toExist();
  });
});
