"use strict";
function StreamingMedia() {
}

StreamingMedia.prototype.playAudio = function (url, options) {
	options = options || {};
	cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "playAudio", [url, options]);
};

StreamingMedia.prototype.stopAudio = function (options) {
    options = options || {};
    cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "stopAudio", [options]);
};

StreamingMedia.prototype.playVideo = function (url, options) {
	options = options || {};
	cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "playVideo", [url, options]);
};


StreamingMedia.install = function () {
	if (!window.plugins) {
		window.plugins = {};
	}
	window.plugins.streamingMedia = new StreamingMedia();
	return window.plugins.streamingMedia;
};

cordova.addConstructor(StreamingMedia.install);