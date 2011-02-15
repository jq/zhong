//
//  ringtonesAppDelegate.h
//  ringtones
//
//  Created by Feng on 2/12/11.
//  Copyright 2011 ringDroid. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface ringtonesAppDelegate : NSObject <UIApplicationDelegate, UIWebViewDelegate>{
    UIWindow *window;
	IBOutlet UIWebView *webView;
	IBOutlet UIImageView *splashView;
	IBOutlet UIActivityIndicatorView *loadingIndicator;
	IBOutlet UILabel *hint;
	
	NSURLRequest *request;
}

@property (nonatomic, retain) IBOutlet UIWindow *window;

@end

