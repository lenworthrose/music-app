package com.lenworthrose.music.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.Constants;

/**
 * Bar displayed at the bottom of the {@link com.lenworthrose.music.activity.MainActivity} while browsing
 * the library.
 */
public class NowPlayingBar extends LinearLayout {
    private boolean isVisible;
    private TextView title, subtitle;
    private ImageView playPause, coverArt;
    private PlaybackService playbackService;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.PLAYBACK_STATE_CHANGED:
                    playbackStateChanged(intent);
                    break;
                case Constants.PLAYING_NOW_CHANGED:
                    playingItemChanged(intent);
                    break;
            }
        }
    };

    public NowPlayingBar(Context context) {
        super(context);
        init(context);
    }

    public NowPlayingBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NowPlayingBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.now_playing_bar, this);
        setAlpha(0f);
        setVisibility(View.GONE);

        title = (TextView)findViewById(R.id.np_title);
        subtitle = (TextView)findViewById(R.id.np_subtitle);
        coverArt = (ImageView)findViewById(R.id.np_cover);
        playPause = (ImageView)findViewById(R.id.np_play_pause);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playbackService.playPause();
            }
        });

        findViewById(R.id.np_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playbackService.next();
            }
        });
    }

    public void setPlaybackService(PlaybackService service) {
        playbackService = service;

        if (service != null) {
            IntentFilter filter = new IntentFilter(Constants.PLAYING_NOW_CHANGED);
            filter.addAction(Constants.PLAYBACK_STATE_CHANGED);
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);

            playingItemChanged(playbackService.getPlayingItemIntent());
            playbackStateChanged(playbackService.getPlaybackStateIntent());
        } else {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
        }
    }

    public void playingItemChanged(Intent intent) {
        String titleString = intent.getStringExtra(Constants.EXTRA_TITLE);

        if (TextUtils.isEmpty(titleString)) {
            if (isVisible) {
                isVisible = false;
                animate().alpha(0f).setDuration(400).setStartDelay(100).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        setVisibility(View.GONE);
                    }
                });
            }

            title.setText("");
            subtitle.setText("");
            coverArt.setImageResource(android.R.color.transparent);
        } else {
            if (!isVisible) {
                isVisible = true;
                animate().alpha(1f).setDuration(1000).setStartDelay(750).withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        setAlpha(0f);
                        setVisibility(View.VISIBLE);
                    }
                });
            }

            title.setText(titleString);
            String subtitle = intent.getStringExtra(Constants.EXTRA_ARTIST);
            String album = intent.getStringExtra(Constants.EXTRA_ALBUM);

            if (!TextUtils.isEmpty(album)) {
                if (!subtitle.isEmpty()) subtitle += " - ";
                subtitle += album;
            }

            this.subtitle.setText(subtitle);
            Glide.with(getContext()).load(intent.getStringExtra(Constants.EXTRA_ALBUM_ART_URL)).fallback(R.drawable.logo).error(R.drawable.logo).into(coverArt);
        }
    }

    public void playbackStateChanged(Intent intent) {
        Constants.PlaybackState state = (Constants.PlaybackState)intent.getSerializableExtra(Constants.EXTRA_STATE);

        switch (state) {
            case BUFFERING:
                playPause.setEnabled(false);
                playPause.setImageResource(R.drawable.play);
                break;
            case PLAYING:
                playPause.setEnabled(true);
                playPause.setImageResource(R.drawable.pause);
                break;
            default:
                playPause.setEnabled(true);
                playPause.setImageResource(R.drawable.play);
                break;
        }
    }
}
