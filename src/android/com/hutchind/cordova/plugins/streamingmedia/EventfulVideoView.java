package com.hutchind.cordova.plugins.streamingmedia;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

import java.util.Optional;

public class EventfulVideoView extends VideoView {

    private PlayPauseListener playPauseListener;
    private SeekToListener seekToListener;

    private Integer customDuration;
    private int offsetPosition;

    public EventfulVideoView(Context context) {
        super(context);
    }

    public EventfulVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EventfulVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EventfulVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setPlayPauseListener(PlayPauseListener listener) {
        playPauseListener = listener;
    }

    public void setSeekToListener(SeekToListener listener) {
        seekToListener = listener;
    }

    @Override
    public int getDuration() {
        return Optional.ofNullable(customDuration).orElse(super.getDuration());
    }

    public void setDuration(Integer duration) {
        this.customDuration = duration;
    }

    @Override
    public int getCurrentPosition() {
        return super.getCurrentPosition() - offsetPosition;
    }

    @Override
    public void pause() {
        super.pause();
        if (playPauseListener != null) {
            playPauseListener.onStreamPause();
        }
    }

    @Override
    public void start() {
        super.start();
        if (playPauseListener != null) {
            playPauseListener.onStreamPlay();
        }
    }

    @Override
    public void seekTo(int msec) {
        if(msec < super.getDuration()) {
            super.seekTo(msec);
            offsetPosition = 0;
        } else {
            offsetPosition = super.getCurrentPosition() - msec;
        }

        if(seekToListener != null) {
            seekToListener.onStreamSeekTo(msec);
        }
    }

    public static interface PlayPauseListener {
        void onStreamPlay();
        void onStreamPause();
    }

    public static interface SeekToListener {
        void onStreamSeekTo(int msec);
    }

}
