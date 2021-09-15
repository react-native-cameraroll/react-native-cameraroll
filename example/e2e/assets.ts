import {promisify} from 'util';
import {join} from 'path';
import {readdir as fsReaddir, lstat as fsLstat} from 'fs';

const readdir = promisify(fsReaddir);
const lstat = promisify(fsLstat);

export interface E2EAsset {
  folderPath?: string;
  fileName: string;
  absolutePath: string;
}

export const getE2EAssets = async (
  absoluteFolderPath?: string,
  folderPath: string = '',
): Promise<E2EAsset[]> => {
  const assetsPath = absoluteFolderPath || join(__dirname, '../assets');
  const assetFileNames: string[] = await readdir(assetsPath);
  let assets: E2EAsset[] = [];

  for (const fileName of assetFileNames) {
    const absolutePath = join(assetsPath, fileName);
    if ((await lstat(absolutePath)).isDirectory()) {
      assets = assets.concat(
        await getE2EAssets(absolutePath, join(folderPath, fileName)),
      );
    } else {
      assets.push({fileName, absolutePath, folderPath});
    }
  }

  return assets;
};
