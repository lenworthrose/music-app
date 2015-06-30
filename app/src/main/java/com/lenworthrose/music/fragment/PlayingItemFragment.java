package com.lenworthrose.music.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.lenworthrose.music.R;
import com.lenworthrose.music.activity.PlayingNowActivity;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.playback.PlayingItem;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Constants.PlaybackState;
import com.lenworthrose.music.util.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This Fragment displays information about the currently playing track (Artist, Album, Title, Cover
 * Art, etc). It also provides playback controls (Play/Pause, Next, Previous, Stop).
 */
public class PlayingItemFragment extends Fragment implements ServiceConnection {
    private SeekBar positionBar;
    private TextView artist, album, title, playlistPosition, playlistTracks, positionDisplay, durationDisplay;
    private ImageView coverArt, playPause;
    private View artistAlbumContainer, ratingSleepTimerContainer, topDetailContainer, bottomDetailContainer;
    private boolean isPositionBarTouched, autoHideOverlays;
    private ScheduledExecutorService scheduleService;
    private ScheduledFuture<?> hideOverlaysFuture;
    private boolean areOverlaysVisible = true;
    private Animation pauseBlinkAnimation = new AlphaAnimation(0f, 1f);
    private CoverArtFadeListener fadeListener = new CoverArtFadeListener();
    private PlaybackService playbackService;
    private Handler handler;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.PLAYING_NOW_CHANGED:
                    playingItemChanged();
                    break;
                case Constants.PLAYBACK_STATE_CHANGED:
                    playbackStateChanged();
                    break;
                case Constants.PLAYING_NOW_PLAYLIST_CHANGED:
                    playlistUpdated();
                    break;
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isPositionBarTouched = false;
            positionDisplay.setText(Utils.longToTimeDisplay(seekBar.getProgress()));
            playbackService.seek(seekBar.getProgress());
            scheduleHideOverlays();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isPositionBarTouched = true;

            if (autoHideOverlays) {
                setOverlaysVisible(true);
                cancelHideOverlays();
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) positionDisplay.setText(Utils.longToTimeDisplay(progress));
        }
    };

    private View.OnClickListener playbackButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.pn_previous:
                    playbackService.previous();
                    break;
                case R.id.pn_play_pause:
                    playbackService.playPause();
                    break;
                case R.id.pn_stop:
                    playbackService.stop();
                    break;
                case R.id.pn_next:
                    playbackService.next();
                    break;
            }
        }
    };

    private View.OnClickListener coverArtClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            cancelHideOverlays();
            setOverlaysVisible(!areOverlaysVisible);

            if (areOverlaysVisible) scheduleHideOverlays();
        }
    };

    private View.OnLongClickListener searchLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.pn_artist:
                    performSearch(artist.getText().toString(), null, null);
                    break;
                case R.id.pn_album:
                case R.id.pn_coverArt:
                    performSearch(artist.getText().toString(), album.getText().toString(), null);
                    break;
                case R.id.pn_name:
                    performSearch(artist.getText().toString(), null, title.getText().toString());
                    break;
            }

            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        autoHideOverlays = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Constants.SETTING_AUTO_HIDE_PLAYING_NOW_OVERLAYS, false);
        handler = new Handler(Looper.getMainLooper());

        if (autoHideOverlays) scheduleService = Executors.newSingleThreadScheduledExecutor();
        setHasOptionsMenu(true);
        pauseBlinkAnimation.setDuration(375);
        pauseBlinkAnimation.setStartOffset(250);
        pauseBlinkAnimation.setRepeatCount(Animation.INFINITE);
        pauseBlinkAnimation.setRepeatMode(Animation.REVERSE);

        getActivity().bindService(new Intent(getActivity(), PlaybackService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing_item, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        topDetailContainer = view.findViewById(R.id.pn_top_detail_container);
        artistAlbumContainer = topDetailContainer.findViewById(R.id.pn_artist_album_container);
        artist = (TextView)artistAlbumContainer.findViewById(R.id.pn_artist);
        artist.setOnLongClickListener(searchLongClickListener);
        album = (TextView)artistAlbumContainer.findViewById(R.id.pn_album);
        album.setOnLongClickListener(searchLongClickListener);
        title = (TextView)topDetailContainer.findViewById(R.id.pn_name);
        title.setOnLongClickListener(searchLongClickListener);
        bottomDetailContainer = view.findViewById(R.id.pn_bottom_detail_container);
        playlistPosition = (TextView)bottomDetailContainer.findViewById(R.id.pn_playlist_position);
        playlistTracks = (TextView)bottomDetailContainer.findViewById(R.id.pn_playlist_tracks);
        positionDisplay = (TextView)bottomDetailContainer.findViewById(R.id.pn_position_display);
        durationDisplay = (TextView)bottomDetailContainer.findViewById(R.id.pn_duration);
        positionBar = (SeekBar)bottomDetailContainer.findViewById(R.id.pn_position_seekbar);
        positionBar.setOnSeekBarChangeListener(seekListener);
        positionBar.setEnabled(false);
        ratingSleepTimerContainer = bottomDetailContainer.findViewById(R.id.pn_rating_sleep_timer_container);
        coverArt = (ImageView)view.findViewById(R.id.pn_coverArt);
        coverArt.setOnClickListener(coverArtClickListener);
        coverArt.setOnLongClickListener(searchLongClickListener);
        View playbackControlContainer = view.findViewById(R.id.pn_playback_control_container);
        playPause = (ImageView)playbackControlContainer.findViewById(R.id.pn_play_pause);
        playPause.setOnClickListener(playbackButtonClickListener);
        playbackControlContainer.findViewById(R.id.pn_previous).setOnClickListener(playbackButtonClickListener);
        playbackControlContainer.findViewById(R.id.pn_next).setOnClickListener(playbackButtonClickListener);
        playbackControlContainer.findViewById(R.id.pn_stop).setOnClickListener(playbackButtonClickListener);

        ShapeDrawable.ShaderFactory topShaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(0, height, 0, 0,
                        new int[] { 0x00090909, 0x29090909, 0x49090909, 0x69090909, 0x7C090909, 0x8A090909, 0x9D090909, 0xB2090909 },
                        new float[] { 0f, .023f, .039f, .056f, .080f, .110f, .165f, 1f },
                        Shader.TileMode.CLAMP);
            }
        };

        PaintDrawable topBackground = new PaintDrawable();
        topBackground.setShape(new RectShape());
        topBackground.setShaderFactory(topShaderFactory);
        topDetailContainer.setBackground(topBackground);

        ShapeDrawable.ShaderFactory bottomShaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(0, 0, 0, height,
                        new int[] { 0x00090909, 0x4D090909, 0x69090909, 0x72090909, 0x76090909, 0x79090909, 0x87090909, 0x93090909, 0xA6090909, 0xB2090909 },
                        new float[] { 0f, .040f, .064f, .083f, .090f, .095f, .12f, .15f, .2f, 1f },
                        Shader.TileMode.CLAMP);
            }
        };

        PaintDrawable bottomBackground = new PaintDrawable();
        bottomBackground.setShape(new RectShape());
        bottomBackground.setShaderFactory(bottomShaderFactory);
        bottomDetailContainer.setBackground(bottomBackground);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, Utils.createPlaybackIntentFilter());
        if (playbackService != null) playbackStateChanged();
    }

    @Override
    public void onPause() {
        cancelHideOverlays();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        getActivity().unbindService(this);
        super.onDestroy();
    }

    public void playingItemChanged() {
        if (isDetached()) return;
        if (autoHideOverlays) setOverlaysVisible(true);

        PlayingItem item = playbackService.getPlayingItem();
        artist.setText(item.getArtist());
        album.setText(item.getAlbum());
        title.setText(item.getTitle());

        if (!playbackService.isPlaylistEmpty()) {
            playlistPosition.setText(String.valueOf(item.getPlaylistPosition()));
            playlistTracks.setText(String.valueOf(playbackService.getPlaylistSize()));
            positionDisplay.setText(R.string.blank_time);

            fadeListener.reset();
            coverArt.animate().alpha(0.08f).setDuration(200).withEndAction(fadeListener);

            Glide.with(this).load(item.getAlbumArtUrl()).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap art, GlideAnimation<? super Bitmap> glideAnimation) {
                    if (art != null) {
                        Utils.createDropShadowBitmap(art, new Utils.BitmapCallback() {
                            @Override
                            public void onBitmapReady(Bitmap bitmap) {
                                fadeListener.setNewCoverArt(bitmap);
                            }
                        });

                        if (getActivity() instanceof PlayingNowActivity)
                            Utils.createBlurredBitmap(art, new Utils.BitmapCallback() {
                                @Override
                                public void onBitmapReady(Bitmap bitmap) {
                                    ((PlayingNowActivity)getActivity()).setBackgroundImage(bitmap);
                                }
                            });
                    } else {
                        resetToLogo();
                    }
                }
            });
        } else {
            resetToLogo();
            positionBar.setEnabled(false);
            positionBar.setProgress(0);
            playlistPosition.setText("0");
            playlistTracks.setText("0");
        }

        artistAlbumContainer.setVisibility(artist.getText().toString().isEmpty() && album.getText().toString().isEmpty() ? View.GONE : View.VISIBLE);
        playbackStateChanged();
    }

    public void playbackStateChanged() {
        if (isDetached()) return;

        PlaybackState state = playbackService.getState();
        playPause.setImageResource(R.drawable.play);

        switch (state) {
            case Stopped:
                positionDisplay.setText(R.string.blank_time);
                durationDisplay.setText(R.string.blank_time);
                positionBar.setProgress(0);
                positionBar.setEnabled(false);
                playPause.setEnabled(true);
                playPause.setAlpha(1f);
                positionDisplay.clearAnimation();
                break;
            case Playing:
                positionBar.setEnabled(true);
                playPause.setAlpha(1f);
                playPause.setImageResource(R.drawable.pause);
                playPause.setEnabled(true);
                scheduleHideOverlays();
                positionDisplay.clearAnimation();

                int duration = playbackService.getDuration();

                if (duration < 0) {
                    positionBar.setProgress(0);
                    positionBar.setEnabled(false);

                    durationDisplay.setText(R.string.blank_time);
                } else {
                    updatePosition();
                    positionBar.setMax(duration);
                    durationDisplay.setText(Utils.longToTimeDisplay(duration));
                }

                handler.postDelayed(new UpdatePositionRunnable(), 333);

                break;
            case Buffering:
                positionBar.setEnabled(false);
                playPause.setEnabled(false);
                playPause.setAlpha(.30f);
                playPause.setImageResource(R.drawable.play);
                positionDisplay.clearAnimation();
                break;
            case Paused:
                positionDisplay.setText(Utils.longToTimeDisplay(playbackService.getPosition()));
                positionDisplay.startAnimation(pauseBlinkAnimation);
                break;
        }

        if (state != PlaybackState.Playing) {
            cancelHideOverlays();
            setOverlaysVisible(true);
        }
    }

    public void playlistUpdated() {
        if (isDetached()) return;

        if (!playbackService.isPlaylistEmpty()) {
            playlistPosition.setText(String.valueOf(playbackService.getPlaylistPositionForDisplay()));
            playlistTracks.setText(String.valueOf(playbackService.getPlaylistSize()));
        } else {
            playlistPosition.setText("0");
            playlistTracks.setText("0");
        }
    }

    private void resetToLogo() {
        fadeListener.setNewCoverArt(BitmapFactory.decodeResource(getResources(), R.drawable.logo));

        if (getActivity() instanceof PlayingNowActivity)
            ((PlayingNowActivity)getActivity()).setBackgroundImage(null);
    }

    private void updatePosition() {
        if (!isPositionBarTouched) {
            int position = playbackService.getPosition();
            positionBar.setProgress(position);
            positionBar.setEnabled(true);
            positionDisplay.setText(Utils.longToTimeDisplay(position));
        }
    }

    private void performSearch(String artist, String album, String name) {

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder)service;
        playbackService = binder.getService();
        playingItemChanged();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }

    private void scheduleHideOverlays() {
        if (!autoHideOverlays || playbackService.getState() != PlaybackState.Playing) return;

        if (hideOverlaysFuture != null)
            hideOverlaysFuture.cancel(false);

        hideOverlaysFuture = scheduleService.schedule(new HideOverlaysTask(), 5, TimeUnit.SECONDS);
    }

    private void cancelHideOverlays() {
        if (hideOverlaysFuture != null) {
            hideOverlaysFuture.cancel(false);
            hideOverlaysFuture = null;
        }
    }

    private void setOverlaysVisible(boolean visible) {
        float alpha = visible ? 1f : 0f;
        int duration = visible ? 400 : 750;
        ViewPropertyAnimator topAnimator = topDetailContainer.animate().alpha(alpha).setDuration(duration);
        ViewPropertyAnimator bottomAnimator = bottomDetailContainer.animate().alpha(alpha).setDuration(duration);

        if (visible) {
            topAnimator.withStartAction(new SetVisibilityRunnable(topDetailContainer, true));
            bottomAnimator.withStartAction(new SetVisibilityRunnable(bottomDetailContainer, true));
        } else {
            topAnimator.withEndAction(new SetVisibilityRunnable(topDetailContainer, false));
            bottomAnimator.withEndAction(new SetVisibilityRunnable(bottomDetailContainer, false));
        }

        topAnimator.start();
        bottomAnimator.start();

        areOverlaysVisible = visible;
    }

    private class HideOverlaysTask implements Runnable {
        @Override
        public void run() {
            topDetailContainer.post(new Runnable() {
                @Override
                public void run() {
                    setOverlaysVisible(false);
                    hideOverlaysFuture = null;
                }
            });
        }
    }

    private static class SetVisibilityRunnable implements Runnable {
        private View view;
        private boolean visible;

        public SetVisibilityRunnable(View view, boolean visible) {
            this.view = view;
            this.visible = visible;
        }

        @Override
        public void run() {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private class CoverArtFadeListener implements Runnable {
        private Bitmap newCoverArt;
        private boolean isFadeOutDone;

        @Override
        public void run() {
            isFadeOutDone = true;
            setImageAndStartFadeIn();
        }

        public void setNewCoverArt(Bitmap newCoverArt) {
            this.newCoverArt = newCoverArt;
            setImageAndStartFadeIn();
        }

        public void reset() {
            newCoverArt = null;
            isFadeOutDone = false;
        }

        private void setImageAndStartFadeIn() {
            if (isFadeOutDone && newCoverArt != null) {
                coverArt.setImageBitmap(newCoverArt);
                coverArt.animate().alpha(1f).setDuration(300);
            }
        }
    }

    private class UpdatePositionRunnable implements Runnable {
        @Override
        public void run() {
            if (playbackService.getState() == PlaybackState.Playing) {
                updatePosition();
                handler.postDelayed(new UpdatePositionRunnable(), 1000);
            }
        }
    }
}
