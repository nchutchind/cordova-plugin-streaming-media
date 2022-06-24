package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.widget.MediaController;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.MotionEvent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.Map;

import org.json.JSONObject;

public class SimpleVideoStream extends Activity implements
MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener,
EventfulVideoView.PlayPauseListener, EventfulVideoView.SeekToListener {

	public static final String BROADCAST_INTENT_ACTION_STOP = "stop_video_stream";

	public static final String CALLBACK_SEEK_KEY_MSEC = "msec";

	private String TAG = getClass().getSimpleName();
	private EventfulVideoView mVideoView = null;
	private MediaPlayer mMediaPlayer = null;
	private MediaController mMediaController = null;
	private ProgressBar mProgressBar = null;
	private String mVideoUrl;
	private Boolean mShouldAutoClose = true;
	private boolean mControls;
	private Map<String, String> headers;
	private Integer customDuration;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if (intent.getAction().equals(BROADCAST_INTENT_ACTION_STOP)) {
			finish();
			}
		}
		};
		registerReceiver(broadcastReceiver, new IntentFilter(BROADCAST_INTENT_ACTION_STOP));

		Bundle b = getIntent().getExtras();
		mVideoUrl = b.getString("mediaUrl");
		mShouldAutoClose = b.getBoolean("shouldAutoClose", true);
		mControls = b.getBoolean("controls", true);
		headers = (Map<String,String>) b.getSerializable(StreamingMedia.INTENT_EXTRA_HEADERS);

		if(b.containsKey("duration"))
			customDuration = b.getInt("duration");

		RelativeLayout relLayout = new RelativeLayout(this);
		relLayout.setBackgroundColor(Color.BLACK);
		RelativeLayout.LayoutParams relLayoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		relLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mVideoView = new EventfulVideoView(this);
		mVideoView.setLayoutParams(relLayoutParam);
		relLayout.addView(mVideoView);

		// Create progress throbber
		mProgressBar = new ProgressBar(this);
		mProgressBar.setIndeterminate(true);
		// Center the progress bar
		RelativeLayout.LayoutParams pblp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		pblp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mProgressBar.setLayoutParams(pblp);
		// Add progress throbber to view
		relLayout.addView(mProgressBar);
		mProgressBar.bringToFront();

		setOrientation(b.getString("orientation"));

		setContentView(relLayout, relLayoutParam);

		play();
	}

	private void play() {
		mProgressBar.setVisibility(View.VISIBLE);
		Uri videoUri = Uri.parse(mVideoUrl);
		try {
			mVideoView.setOnCompletionListener(this);
			mVideoView.setOnPreparedListener(this);
			mVideoView.setOnErrorListener(this);
			mVideoView.setPlayPauseListener(this);
			mVideoView.setSeekToListener(this);
			mVideoView.setVideoURI(videoUri, headers);
			mMediaController = new MediaController(this);
			mMediaController.setAnchorView(mVideoView);
			mMediaController.setMediaPlayer(mVideoView);
			if (!mControls) {
				mMediaController.setVisibility(View.GONE);
			}
			mVideoView.setDuration(customDuration);
			mVideoView.setMediaController(mMediaController);
		} catch (Throwable t) {
			Log.d(TAG, t.toString());
		}
	}

	private void setOrientation(String orientation) {
		if ("landscape".equals(orientation)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}else if("portrait".equals(orientation)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}

	private Runnable checkIfPlaying = new Runnable() {
		@Override
		public void run() {
			if (mVideoView.getCurrentPosition() > 0) {
				// Video is not at the very beginning anymore.
				// Hide the progress bar.
				mProgressBar.setVisibility(View.GONE);
			} else {
				// Video is still at the very beginning.
				// Check again after a small amount of time.
				mVideoView.postDelayed(checkIfPlaying, 100);
			}
		}
	};

	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "Stream is prepared");
		mMediaPlayer = mp;
		mMediaPlayer.setOnBufferingUpdateListener(this);
		mVideoView.requestFocus();
		mVideoView.start();
		mVideoView.postDelayed(checkIfPlaying, 0);
	}

	private void pause() {
		Log.d(TAG, "Pausing video.");
		mVideoView.pause();
	}

	private void stop() {
		Log.d(TAG, "Stopping video.");
		mVideoView.stopPlayback();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy triggered.");
		stop();
	}

	private void wrapItUp(int resultCode, String message) {
		Log.d(TAG, "wrapItUp was triggered.");
		Intent intent = new Intent();
		intent.putExtra("message", message);
		setResult(resultCode, intent);
		finish();
	}

	public void onCompletion(MediaPlayer mp) {
		Log.d(TAG, "onCompletion triggered.");
		stop();
		if (mShouldAutoClose) {
			wrapItUp(RESULT_OK, null);
		}
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		StringBuilder sb = new StringBuilder();
		sb.append("MediaPlayer Error: ");
		switch (what) {
			case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			sb.append("Not Valid for Progressive Playback");
			break;
			case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			sb.append("Server Died");
			break;
			case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			sb.append("Unknown");
			break;
			default:
			sb.append(" Non standard (");
			sb.append(what);
			sb.append(")");
		}
		sb.append(" (" + what + ") ");
		sb.append(extra);
		Log.e(TAG, sb.toString());

		wrapItUp(RESULT_CANCELED, sb.toString());
		return true;
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.d(TAG, "onBufferingUpdate : " + percent + "%");
	}

	@Override
	public void onBackPressed() {
		// If we're leaving, let's finish the activity
		wrapItUp(RESULT_OK, null);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// The screen size changed or the orientation changed... don't restart the activity
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mMediaController != null)
			mMediaController.show();
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();

		try {
			JSONObject callbackData = new JSONObject();
			callbackData.put(StreamingMedia.CALLBACK_EVENT_TYPE_KEY, StreamingMedia.CALLBACK_EVENT_TYPE_VALUE_LIFECYCLE_ONPAUSE);
			StreamingMedia.sendCallback(callbackData);
		} catch(Exception e) {
			Log.e(TAG, "Failed to notify of lifecycle pause event", e);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		try {
			JSONObject callbackData = new JSONObject();
			callbackData.put(StreamingMedia.CALLBACK_EVENT_TYPE_KEY, StreamingMedia.CALLBACK_EVENT_TYPE_VALUE_LIFECYCLE_ONRESUME);
			StreamingMedia.sendCallback(callbackData);
		} catch(Exception e) {
			Log.e(TAG, "Failed to notify of lifecycle resume event", e);
		}
	}

	@Override
	public void onStreamPlay() {
		try {
			JSONObject callbackData = new JSONObject();
			callbackData.put(StreamingMedia.CALLBACK_EVENT_TYPE_KEY, StreamingMedia.CALLBACK_EVENT_TYPE_VALUE_PLAY);
			StreamingMedia.sendCallback(callbackData);
		} catch(Exception e) {
			Log.e(TAG, "Failed to notify of play event", e);
		}
	}

	@Override
	public void onStreamPause() {
		try {
			JSONObject callbackData = new JSONObject();
			callbackData.put(StreamingMedia.CALLBACK_EVENT_TYPE_KEY, StreamingMedia.CALLBACK_EVENT_TYPE_VALUE_PAUSE);
			StreamingMedia.sendCallback(callbackData);
		} catch(Exception e) {
			Log.e(TAG, "Failed to notify of pause event", e);
		}
	}

	@Override
	public void onStreamSeekTo(int msec) {
		try {
			JSONObject callbackData = new JSONObject();
			callbackData.put(StreamingMedia.CALLBACK_EVENT_TYPE_KEY, StreamingMedia.CALLBACK_EVENT_TYPE_VALUE_SEEK);
			callbackData.put(CALLBACK_SEEK_KEY_MSEC, msec);
			StreamingMedia.sendCallback(callbackData);
		} catch(Exception e) {
			Log.e(TAG, "Failed to notify of seek event", e);
		}
	}

}