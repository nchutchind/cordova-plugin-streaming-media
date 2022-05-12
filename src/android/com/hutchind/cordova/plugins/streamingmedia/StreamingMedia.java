package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Build;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

public class StreamingMedia extends CordovaPlugin {

	public static final String CALLBACK_EVENT_TYPE_KEY = "eventType";
	public static final String CALLBACK_EVENT_TYPE_VALUE_LIFECYCLE_ONPAUSE = "LIFECYCLE_ONPAUSE";
	public static final String CALLBACK_EVENT_TYPE_VALUE_LIFECYCLE_ONRESUME = "LIFECYCLE_ONRESUME";
	public static final String CALLBACK_EVENT_TYPE_VALUE_PLAY = "PLAY";
	public static final String CALLBACK_EVENT_TYPE_VALUE_PAUSE = "PAUSE";
	public static final String CALLBACK_EVENT_TYPE_VALUE_SEEK = "SEEK";

	public static final String ACTION_PLAY_AUDIO = "playAudio";
	public static final String ACTION_PLAY_VIDEO = "playVideo";

	public static final String INTENT_EXTRA_HEADERS = "headers";

	private static final int ACTIVITY_CODE_PLAY_MEDIA = 7;

	static CallbackContext callbackContext;

	private static final String TAG = "StreamingMediaPlugin";


	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		StreamingMedia.callbackContext = callbackContext;
		JSONObject options = null;

		try {
			options = args.getJSONObject(1);
		} catch (JSONException e) {
			// Developer provided no options. Leave options null.
		}

		if (ACTION_PLAY_AUDIO.equals(action)) {
			return playAudio(args.getString(0), options);
		} else if (ACTION_PLAY_VIDEO.equals(action)) {
			return playVideo(args.getString(0), options);
		} else {
			callbackContext.error("streamingMedia." + action + " is not a supported method.");
			return false;
		}
	}

	private boolean playAudio(String url, JSONObject options) {
		return play(SimpleAudioStream.class, url, options);
	}
	private boolean playVideo(String url, JSONObject options) {
		return play(SimpleVideoStream.class, url, options);
	}

	private boolean play(final Class activityClass, final String url, final JSONObject options) {
		final CordovaInterface cordovaObj = cordova;
		final CordovaPlugin plugin = this;

		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				final Intent streamIntent = new Intent(cordovaObj.getActivity().getApplicationContext(), activityClass);
				Bundle extras = new Bundle();
				extras.putString("mediaUrl", url);

				if (options != null) {
					Iterator<String> optKeys = options.keys();
					while (optKeys.hasNext()) {
						try {
							final String optKey = (String)optKeys.next();
							if (options.get(optKey).getClass().equals(String.class)) {
								extras.putString(optKey, (String)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							} else if (options.get(optKey).getClass().equals(Boolean.class)) {
								extras.putBoolean(optKey, (Boolean)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							} else if (options.get(optKey).getClass().equals(Integer.class)) {
								extras.putInt(optKey, (Integer)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							}
						} catch (JSONException e) {
							Log.e(TAG, "JSONException while trying to read options. Skipping option.");
						}
					}

					// Header options
					try {
						JSONObject headersJSON = options.getJSONObject("headers");
						Map<String,String> headers = new HashMap<>();
						Iterator<String> headersJSONIterator = headersJSON.keys();
						while(headersJSONIterator.hasNext()) {
							String key = headersJSONIterator.next();
							headers.put(key, headersJSON.getString(key));
						}
						extras.putSerializable(INTENT_EXTRA_HEADERS, (Serializable) headers);
						Log.v(TAG, "Added option: headers -> " + headersJSON.toString());
					} catch(JSONException e) {
						// Do nothing, headers aren't present
					}

					streamIntent.putExtras(extras);
				}

				cordovaObj.startActivityForResult(plugin, streamIntent, ACTIVITY_CODE_PLAY_MEDIA);
			}
		});
		return true;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.v(TAG, "onActivityResult: " + requestCode + " " + resultCode);
		super.onActivityResult(requestCode, resultCode, intent);
		if (ACTIVITY_CODE_PLAY_MEDIA == requestCode) {
			if (Activity.RESULT_OK == resultCode) {
				StreamingMedia.callbackContext.success();
			} else if (Activity.RESULT_CANCELED == resultCode) {
				String errMsg = "Error";
				if (intent != null && intent.hasExtra("message")) {
					errMsg = intent.getStringExtra("message");
				}
				StreamingMedia.callbackContext.error(errMsg);
			}
		}
	}

	public static void sendCallback(JSONObject callbackData) {
		PluginResult result = new PluginResult(PluginResult.Status.OK, callbackData);
		result.setKeepCallback(true);
		StreamingMedia.callbackContext.sendPluginResult(result);
	}
}