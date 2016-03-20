# Cordova Streaming Media plugin

for iOS and Android, by [Nicholas Hutchind](https://github.com/nchutchind)

## Description

This plugin allows you to stream audio and video in a fullscreen, native player on iOS and Android.

* 1.0.0 Works with Cordova 3.x
* 1.0.1+ Works with Cordova >= 4.0

## Installation

```
cordova plugin add cordova-plugin-streaming-media
```

### iOS specifics
* Uses the MPMoviePlayerController.
* Tested on iOS 7. (If someone has an iOS 6 device, please report back to me how it does.)

### Android specifics
* Uses VideoView and MediaPlayer.
* Creates two activities in your AndroidManifest.xml file.
* Tested on Android 4.0+. (If someone has a Gingerbread device, please report back to me how it does.)

## Usage

```javascript
  var videoUrl = STREAMING_VIDEO_URL;

  // Just play a video
  window.plugins.streamingMedia.playVideo(videoUrl);

  // Play a video with callbacks
  var options = {
    successCallback: function() {
      console.log("Video was closed without error.");
    },
    errorCallback: function(errMsg) {
      console.log("Error! " + errMsg);
    },
    orientation: 'landscape'
  };
  window.plugins.streamingMedia.playVideo(videoUrl, options);


  var audioUrl = STREAMING_AUDIO_URL;

  // Play an audio file (not recommended, since the screen will be plain black)
  window.plugins.streamingMedia.playAudio(audioUrl);

  // Play an audio file with options (all options optional)
  var options = {
    bgColor: "#FFFFFF",
    bgImage: "<SWEET_BACKGROUND_IMAGE>",
    bgImageScale: "fit", // other valid values: "stretch"
    initFullscreen: false, // true(default)/false iOS only
    successCallback: function() {
      console.log("Player closed without error.");
    },
    errorCallback: function(errMsg) {
      console.log("Error! " + errMsg);
    }
  };
  window.plugins.streamingMedia.playAudio(audioUrl, options);


  // Stop current audio
  window.plugins.streamingMedia.stopAudio();

```
