#import "StreamingMedia.h"
#import <Cordova/CDV.h>

@interface StreamingMedia()
	- (void)parseOptions:(NSDictionary *) options type:(NSString *) type;
	- (void)play:(CDVInvokedUrlCommand *) command type:(NSString *) type;
	- (void)setBackgroundColor:(NSString *)color;
	- (void)setImage:(NSString*)imagePath withScaleType:(NSString*)imageScaleType;
	- (UIImage*)getImage: (NSString *)imageName;
	- (void)startPlayer:(NSString*)uri;
	- (void)moviePlayBackDidFinish:(NSNotification*)notification;
	- (void)cleanup;
@end

@implementation StreamingMedia {
	NSString* callbackId;
	MPMoviePlayerController *moviePlayer;
	BOOL shouldAutoClose;
	UIColor *backgroundColor;
	UIImageView *imageView;
    BOOL *initFullscreen;
}

NSString * const TYPE_VIDEO = @"VIDEO";
NSString * const TYPE_AUDIO = @"AUDIO";
NSString * const DEFAULT_IMAGE_SCALE = @"center";

-(void)parseOptions:(NSDictionary *)options type:(NSString *) type {
	// Common options
	if (![options isKindOfClass:[NSNull class]] && [options objectForKey:@"shouldAutoClose"]) {
		shouldAutoClose = [[options objectForKey:@"shouldAutoClose"] boolValue];
	} else {
		shouldAutoClose = true;
	}
	if (![options isKindOfClass:[NSNull class]] && [options objectForKey:@"bgColor"]) {
		[self setBackgroundColor:[options objectForKey:@"bgColor"]];
	} else {
		backgroundColor = [UIColor blackColor];
	}

    if (![options isKindOfClass:[NSNull class]] && [options objectForKey:@"initFullscreen"]) {
        initFullscreen = [[options objectForKey:@"initFullscreen"] boolValue];
    } else {
        initFullscreen = true;
    }

	if ([type isEqualToString:TYPE_AUDIO]) {
		// bgImage
		// bgImageScale
		if (![options isKindOfClass:[NSNull class]] && [options objectForKey:@"bgImage"]) {
			NSString *imageScale = DEFAULT_IMAGE_SCALE;
			if (![options isKindOfClass:[NSNull class]] && [options objectForKey:@"bgImageScale"]) {
				imageScale = [options objectForKey:@"bgImageScale"];
			}
			[self setImage:[options objectForKey:@"bgImage"] withScaleType:imageScale];
		}
		// bgColor
		if (![options isKindOfClass:[NSNull class]] && [options objectForKey:@"bgColor"]) {
			NSLog(@"Found option for bgColor");
			[self setBackgroundColor:[options objectForKey:@"bgColor"]];
		} else {
			backgroundColor = [UIColor blackColor];
		}
	}
	// No specific options for video yet
}

-(void)play:(CDVInvokedUrlCommand *) command type:(NSString *) type {
	callbackId = command.callbackId;
	NSString *mediaUrl  = [command.arguments objectAtIndex:0];
	[self parseOptions:[command.arguments objectAtIndex:1] type:type];

	[self startPlayer:mediaUrl];
}

-(void)stop:(CDVInvokedUrlCommand *) command type:(NSString *) type {
    callbackId = command.callbackId;
    if (moviePlayer) {
        [moviePlayer stop];
    }
}

-(void)playVideo:(CDVInvokedUrlCommand *) command {
	[self play:command type:[NSString stringWithString:TYPE_VIDEO]];
}

-(void)playAudio:(CDVInvokedUrlCommand *) command {
	[self play:command type:[NSString stringWithString:TYPE_AUDIO]];
}

-(void)stopAudio:(CDVInvokedUrlCommand *) command {
    [self stop:command type:[NSString stringWithString:TYPE_AUDIO]];
}

-(void) setBackgroundColor:(NSString *)color {
	if ([color hasPrefix:@"#"]) {
		// HEX value
		unsigned rgbValue = 0;
		NSScanner *scanner = [NSScanner scannerWithString:color];
		[scanner setScanLocation:1]; // bypass '#' character
		[scanner scanHexInt:&rgbValue];
		backgroundColor = [UIColor colorWithRed:((float)((rgbValue & 0xFF0000) >> 16))/255.0 green:((float)((rgbValue & 0xFF00) >> 8))/255.0 blue:((float)(rgbValue & 0xFF))/255.0 alpha:1.0];
	} else {
		// Color name
		NSString *selectorString = [[color lowercaseString] stringByAppendingString:@"Color"];
		SEL selector = NSSelectorFromString(selectorString);
		UIColor *colorObj = [UIColor blackColor];
		if ([UIColor respondsToSelector:selector]) {
			colorObj = [UIColor performSelector:selector];
		}
		backgroundColor = colorObj;
	}
}

-(UIImage*)getImage: (NSString *)imageName {
	UIImage *image = nil;
	if (imageName != (id)[NSNull null]) {
		if ([imageName hasPrefix:@"http"]) {
			// Web image
			image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL URLWithString:imageName]]];
		} else if ([imageName hasPrefix:@"www/"]) {
			// Asset image
			image = [UIImage imageNamed:imageName];
		} else if ([imageName hasPrefix:@"file://"]) {
			// Stored image
			image = [UIImage imageWithData:[NSData dataWithContentsOfFile:[[NSURL URLWithString:imageName] path]]];
		} else if ([imageName hasPrefix:@"data:"]) {
			// base64 encoded string
			NSURL *imageURL = [NSURL URLWithString:imageName];
			NSData *imageData = [NSData dataWithContentsOfURL:imageURL];
			image = [UIImage imageWithData:imageData];
		} else {
			// explicit path
			image = [UIImage imageWithData:[NSData dataWithContentsOfFile:imageName]];
		}
	}
	return image;
}

- (void)orientationChanged:(NSNotification *)notification {
	if (imageView != nil) {
		// adjust imageView for rotation
		imageView.bounds = moviePlayer.backgroundView.bounds;
		imageView.frame = moviePlayer.backgroundView.frame;
	}
}

-(void)setImage:(NSString*)imagePath withScaleType:(NSString*)imageScaleType {
	imageView = [[UIImageView alloc] initWithFrame:self.viewController.view.bounds];
	if (imageScaleType == nil) {
		NSLog(@"imagescaletype was NIL");
		imageScaleType = DEFAULT_IMAGE_SCALE;
	}
	if ([imageScaleType isEqualToString:@"stretch"]){
		// Stretches image to fill all available background space, disregarding aspect ratio
		imageView.contentMode = UIViewContentModeScaleToFill;
		moviePlayer.backgroundView.contentMode = UIViewContentModeScaleToFill;
	} else if ([imageScaleType isEqualToString:@"fit"]) {
		// Stretches image to fill all possible space while retaining aspect ratio
		imageView.contentMode = UIViewContentModeScaleAspectFit;
		moviePlayer.backgroundView.contentMode = UIViewContentModeScaleAspectFit;
	} else {
		// Places image in the center of the screen
		imageView.contentMode = UIViewContentModeCenter;
		moviePlayer.backgroundView.contentMode = UIViewContentModeCenter;
	}

	[imageView setImage:[self getImage:imagePath]];
}

-(void)startPlayer:(NSString*)uri {
	NSURL *url = [NSURL URLWithString:uri];

	moviePlayer =  [[MPMoviePlayerController alloc] initWithContentURL:url];

	// Listen for playback finishing
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(moviePlayBackDidFinish:)
												 name:MPMoviePlayerPlaybackDidFinishNotification
											   object:moviePlayer];
	// Listen for click on the "Done" button
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(doneButtonClick:)
												 name:MPMoviePlayerWillExitFullscreenNotification
											   object:nil];
	// Listen for orientation change
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(orientationChanged:)
												 name:UIDeviceOrientationDidChangeNotification
											   object:nil];

	moviePlayer.controlStyle = MPMovieControlStyleDefault;

	moviePlayer.shouldAutoplay = YES;
	if (imageView != nil) {
		[moviePlayer.backgroundView setAutoresizesSubviews:YES];
		[moviePlayer.backgroundView addSubview:imageView];
	}
	moviePlayer.backgroundView.backgroundColor = backgroundColor;
	[self.viewController.view addSubview:moviePlayer.view];

	// Note: animating does a fade to black, which may not match background color
    if (initFullscreen) {
        [moviePlayer setFullscreen:YES animated:NO];
    } else {
        [moviePlayer setFullscreen:NO animated:NO];
    }
}

- (void) moviePlayBackDidFinish:(NSNotification*)notification {
	NSDictionary *notificationUserInfo = [notification userInfo];
	NSNumber *resultValue = [notificationUserInfo objectForKey:MPMoviePlayerPlaybackDidFinishReasonUserInfoKey];
	MPMovieFinishReason reason = [resultValue intValue];
	NSString *errorMsg;
	if (reason == MPMovieFinishReasonPlaybackError) {
		NSError *mediaPlayerError = [notificationUserInfo objectForKey:@"error"];
		if (mediaPlayerError) {
			errorMsg = [mediaPlayerError localizedDescription];
		} else {
			errorMsg = @"Unknown error.";
		}
		NSLog(@"Playback failed: %@", errorMsg);
	}

	if (shouldAutoClose || [errorMsg length] != 0) {
		[self cleanup];
		CDVPluginResult* pluginResult;
		if ([errorMsg length] != 0) {
			pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMsg];
		} else {
			pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:true];
		}
		[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
	}
}

-(void)doneButtonClick:(NSNotification*)notification{
	[self cleanup];

	CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:true];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void)cleanup {
	NSLog(@"Clean up");
	imageView = nil;
    initFullscreen = false;
	backgroundColor = nil;

	// Remove Done Button listener
	[[NSNotificationCenter defaultCenter]
							removeObserver:self
									  name:MPMoviePlayerWillExitFullscreenNotification
									object:nil];
	// Remove playback finished listener
	[[NSNotificationCenter defaultCenter]
							removeObserver:self
									  name:MPMoviePlayerPlaybackDidFinishNotification
									object:moviePlayer];
	// Remove orientation change listener
	[[NSNotificationCenter defaultCenter]
							removeObserver:self
									  name:UIDeviceOrientationDidChangeNotification
									object:nil];

	if (moviePlayer) {
		moviePlayer.fullscreen = NO;
		[moviePlayer setInitialPlaybackTime:-1];
		[moviePlayer stop];
		moviePlayer.controlStyle = MPMovieControlStyleNone;
		[moviePlayer.view removeFromSuperview];
		moviePlayer = nil;
	}
}
@end
