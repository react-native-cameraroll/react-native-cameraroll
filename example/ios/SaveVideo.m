//
//  SaveVideo.m
//  CameraRollExample
//
//  Created by Abdul Basit on 30/04/2019.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "React/RCTBridgeModule.h"
@interface RCT_EXTERN_MODULE(SaveVideo, NSObject)
RCT_EXTERN_METHOD(saveVideoToPhone:(NSString)url)
@end
