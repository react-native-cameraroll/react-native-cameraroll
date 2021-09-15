const child_process = require('child_process');
import {promisify} from 'util';
import {readFile as fsReadFile, writeFile as fsWriteFile} from 'fs';
import {getE2EAssets} from '../example/e2e/assets';
import {wait} from '../example/e2e/utils';

const readFile = promisify(fsReadFile);
const exec = promisify(child_process.exec);
const writeFile = promisify(fsWriteFile);

const AVD_NAME = 'RNCameraRollTestingAVD';
const ANDROID_SDK_CMD_TOOLS_PATH = '$ANDROID_HOME/cmdline-tools/latest/bin';

(async function() {
  // Install SDK
  await exec(
    `${ANDROID_SDK_CMD_TOOLS_PATH}/sdkmanager 'system-images;android-30;google_apis;x86_64'`,
  );

  // Delete AVD if it already exists
  try {
    await exec(
      `${ANDROID_SDK_CMD_TOOLS_PATH}/avdmanager delete avd --name ${AVD_NAME}`,
    );
  } catch (error) {
    if (
      !error.stderr.includes(
        "There is no Android Virtual Device named 'RNCameraRollTestingAVD'",
      )
    ) {
      throw error;
    }
  }

  console.log('Creating AVD');
  // Create AVD
  await exec(
    `echo no | ${ANDROID_SDK_CMD_TOOLS_PATH}/avdmanager create avd --force --name ${AVD_NAME} --abi google_apis/x86_64 --package 'system-images;android-30;google_apis;x86_64' --device pixel_5`,
  );

  // Turn off audio I/O to prevent deadlocks
  const avdConfigFilePath = `${
    process.env.HOME
  }/.android/avd/${AVD_NAME}.avd/config.ini`;
  let configFile = (await readFile(avdConfigFilePath)).toString();
  configFile = configFile.replace('hw.audioInput=yes', 'hw.audioInput=no');
  configFile = configFile.replace('hw.audioOutput=yes', 'hw.audioOutput=no');
  await writeFile(avdConfigFilePath, configFile);

  // Launch emulator, we need it running to use adb later
  const emulator = child_process.spawn('emulator', ['-avd', AVD_NAME]);

  // Wait for emualtor to be ready
  await new Promise<void>(resolve => {
    emulator.stdout.on('data', (chunk: string) => {
      const log = chunk.toString();
      console.log(log);
      if (log.includes('INFO: boot completed')) {
        resolve();
      }
    });
  });
  await wait(5000);

  // Add all assets to the emulators SD card
  const assets = await getE2EAssets();
  for (const asset of assets) {
    await exec(
      `adb push ${asset.absolutePath} /sdcard/${asset.folderPath}/${
        asset.fileName
      }`,
    );
  }
  console.log(`Added ${assets.length} assets to emulator`);

  // Stop the emulator
  emulator.kill();
})();
