"use strict";
function StreamingMedia() {
}

StreamingMedia.prototype.playAudio = function (url, options = {}) {
	cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "playAudio", [url, options]);
};

StreamingMedia.prototype.pauseAudio = function (options = {}) {
    cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "pauseAudio", [options]);
};

StreamingMedia.prototype.resumeAudio = function (options = {}) {
    cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "resumeAudio", [options]);
};

StreamingMedia.prototype.stopAudio = function (options = {}) {
    cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "stopAudio", [options]);
};

StreamingMedia.prototype.playVideo = function (url, options = {}) {
	cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "playVideo", [url, options]);
};

StreamingMedia.prototype.stopVideo = function (options = {}) {
	cordova.exec(options.successCallback || null, options.errorCallback || null, "StreamingMedia", "stopVideo", []);
};


StreamingMedia.install = function () {
	if (!window.plugins) {
		window.plugins = {};
	}
	window.plugins.streamingMedia = new StreamingMedia();
	return window.plugins.streamingMedia;
};

cordova.addConstructor(StreamingMedia.install);