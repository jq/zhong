//
//  ringtonesAppDelegate.m
//  ringtones
//
//  Created by Feng on 2/12/11.
//  Copyright 2011 ringDroid. All rights reserved.
//

#import "ringtonesAppDelegate.h"
@implementation ringtonesAppDelegate

@synthesize window;


#pragma mark -
#pragma mark Application lifecycle

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {    
    
    // Override point for customization after application launch.
		
	request = [NSURLRequest requestWithURL:[NSURL URLWithString:@"http://m.ringdroid.me/iphone_app.html"]
							   cachePolicy:NSURLRequestReturnCacheDataElseLoad
						   timeoutInterval:10.0];

	[webView setDelegate:self];
	[webView loadRequest:request];

	hint.textColor = [UIColor blackColor];
	hint.text = @"Connecting to ringtone Server";

	[self.window makeKeyAndVisible];
		
    return YES;
}


- (void)applicationWillResignActive:(UIApplication *)application {
    /*
     Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
     Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
     */
}


- (void)applicationDidEnterBackground:(UIApplication *)application {
    /*
     Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later. 
     If your application supports background execution, called instead of applicationWillTerminate: when the user quits.
     */

	splashView.hidden = NO;
	
	hint.textColor = [UIColor blackColor];
	hint.text = @"Connecting to ringtone Server";
	hint.hidden = NO;
	
	[webView stopLoading];
	
}


- (void)applicationWillEnterForeground:(UIApplication *)application {
    /*
     Called as part of  transition from the background to the inactive state: here you can undo many of the changes made on entering the background.
     */

}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    /*
     Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
     */
	[loadingIndicator startAnimating];

	[webView loadRequest:request];
	
}


- (void)applicationWillTerminate:(UIApplication *)application {
    /*
     Called when the application is about to terminate.
     See also applicationDidEnterBackground:.
     */
}

#pragma mark -
#pragma mark WebView event Handle
- (void)webViewDidFinishLoad:(UIWebView *)webView {
	
	[loadingIndicator stopAnimating];
	splashView.hidden = YES;
	hint.hidden = YES;
	
}

- (void)webView:(UIWebView *)webView didFailLoadWithError:(NSError *)error
{
	if ([error code] != -999) {
		[loadingIndicator stopAnimating];
		hint.textColor = [UIColor redColor];
		hint.text = @"Connection Error\nHit the Home button, then check the network and try again.";
	}
}


#pragma mark -
#pragma mark Memory management

- (void)applicationDidReceiveMemoryWarning:(UIApplication *)application {
    /*
     Free up as much memory as possible by purging cached data objects that can be recreated (or reloaded from disk) later.
     */
}


- (void)dealloc {
    [window release];
    [super dealloc];
}


@end
