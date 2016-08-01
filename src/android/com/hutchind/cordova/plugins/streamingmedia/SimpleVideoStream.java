package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.widget.MediaController;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.MotionEvent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import android.widget.Button;
import java.util.ArrayList;

public class SimpleVideoStream extends Activity implements
	MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
	MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {
	private String TAG = getClass().getSimpleName();
	private VideoView mVideoView = null;
	private MediaPlayer mMediaPlayer = null;
	private MediaController mMediaController = null;
	private ProgressBar mProgressBar = null;
	private String mVideoUrl;
	private Boolean mShouldAutoClose = true;
	private int videoCount = 0;
	private int playlistCount = 0;
	private ArrayList videos;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Bundle b = getIntent().getExtras();
		mVideoUrl = b.getString("mediaUrl");
		playlistCount = new Integer(b.getString("playlistCount"));
		videos = new ArrayList();
		for(int i=0; i< playlistCount; i++){
			videos.add(b.getString("v" + String.valueOf(i)));
		}
		mShouldAutoClose = b.getBoolean("shouldAutoClose");
		mShouldAutoClose = mShouldAutoClose == null ? true : mShouldAutoClose;

		RelativeLayout relLayout = new RelativeLayout(this);
		String backgroundColor = b.getString("bgColor");
		relLayout.setBackgroundColor(Color.parseColor(backgroundColor));
		RelativeLayout.LayoutParams relLayoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		relLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mVideoView = new VideoView(this);
		mVideoView.setLayoutParams(relLayoutParam);
		relLayout.addView(mVideoView);

		// Button myButton = new Button(this);
		// myButton.setId(3);
		// android.widget.RelativeLayout.LayoutParams buttonParams =
  //  		new android.widget.RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
  //    	RelativeLayout.LayoutParams.WRAP_CONTENT);
		// buttonParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		// myButton.setLayoutParams(buttonParams);
		// myButton.setText("My Button!");
		// relLayout.addView(myButton);

		// Create progress throbber
		mProgressBar = new ProgressBar(this);
		mProgressBar.setIndeterminate(true);
		// Center the progress bar
		RelativeLayout.LayoutParams pblp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		pblp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mProgressBar.setLayoutParams(pblp);
		// mProgressBar.getIndeterminateDrawable().setColorFilter(
  //   Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
		// Add progress throbber to view
		relLayout.addView(mProgressBar);
		mProgressBar.bringToFront();

		setOrientation(b.getString("orientation"));

		setContentView(relLayout, relLayoutParam);

		play(mVideoUrl);
	}

	@Override
	public void onPause() {
    	super.onPause();  // Always call the superclass method first

    // Release the Camera because we don't need it when paused
   		mVideoView.pause();
	}

	@Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first

	    
	    
	}

	private void play(String url) {
		mProgressBar.setVisibility(View.VISIBLE);
		Uri videoUri = Uri.parse(url);
		try {
			mVideoView.setOnCompletionListener(this);
			mVideoView.setOnPreparedListener(this);
			mVideoView.setOnErrorListener(this);
			mVideoView.setVideoURI(videoUri);
			mMediaController = new MediaController(this);
			mMediaController.setAnchorView(mVideoView);
			mMediaController.setMediaPlayer(mVideoView);
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
		stop();
	}

	private void wrapItUp(int resultCode, String message) {
		Intent intent = new Intent();
		intent.putExtra("message", message);
		setResult(resultCode, intent);
		finish();
	}

	public void onCompletion(MediaPlayer mp) {
		stop();
		if (mShouldAutoClose) {
			if(videoCount > playlistCount - 1){
				wrapItUp(RESULT_CANCELED, "completion");
			} else{
				this.play((String)videos.get(videoCount));
				videoCount++;
			}
			//this.play(nextVideoUrl);
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
		if (mMediaController != null){
			if(mMediaController.isShowing()){
				mMediaController.hide();
			} else{
				mMediaController.show();
			}
		}
		return false;
	}
}
