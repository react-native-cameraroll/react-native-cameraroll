//
//  SaveVideo.swift
//  CameraRollExample
//
//  Created by Abdul Basit on 30/04/2019.
//  Copyright © 2019 Facebook. All rights reserved.
//

import Foundation
import  Photos
@objc(SaveVideo)
class SaveVideo: NSObject {
  
  @objc
  func saveVideoToPhone(_ url: NSString) {
    print("name", url)
    let sampleURL = url;
    DispatchQueue.global(qos: .background).async {
      if let url = URL(string: sampleURL as String), let urlData = NSData(contentsOf: url) {
        let galleryPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0];
        let filePath="\(galleryPath)/nameX.mp4";
        DispatchQueue.main.async {
          urlData.write(toFile: filePath, atomically: true)
          PHPhotoLibrary.shared().performChanges({
            PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL:
              URL(fileURLWithPath: filePath))
          }) {
            success, error in
            if success {
              print("Succesfully Saved")
            } else {
              print(error?.localizedDescription)
            }
          }
        }
      }
    }
  }
  
  @objc
  func constantsToExport() -> [AnyHashable : Any]! {
    return ["initialCount": 0]
  }
}
