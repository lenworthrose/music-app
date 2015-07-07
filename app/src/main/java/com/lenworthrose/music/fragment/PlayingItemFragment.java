package com.lenworthrose.music.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    private View artistAlbumContainer, topDetailContainer, bottomDetailContainer;
    private boolean isPositionBarTouched, autoHideOverlays;
    private ScheduledExecutorService scheduleService;
    private ScheduledFuture<?> hideOverlaysFuture;
    private boolean areOverlaysVisible = true;
    private Animation pauseBlinkAnimation = new AlphaAnimation(0f, 1f);
    private PlaybackService playbackService;
    private Handler handler;
    private MenuItem repeatItem;
    private boolean isRepeatEnabled;
    private int position;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.PLAYING_NOW_CHANGED:
                    playingItemChanged(intent);
                    break;
                case Constants.PLAYBACK_STATE_CHANGED:
                    playbackStateChanged(intent);
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

        setHasOptionsMenu(true); //Required for the system to call onCreateOptionsMenu() to get the Repeat MenuItem
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
        album = (TextView)artistAlbumContainer.findViewById(R.id.pn_album);
        title = (TextView)topDetailContainer.findViewById(R.id.pn_name);
        bottomDetailContainer = view.findViewById(R.id.pn_bottom_detail_container);
        playlistPosition = (TextView)bottomDetailContainer.findViewById(R.id.pn_playlist_position);
        playlistTracks = (TextView)bottomDetailContainer.findViewById(R.id.pn_playlist_tracks);
        positionDisplay = (TextView)bottomDetailContainer.findViewById(R.id.pn_position_display);
        durationDisplay = (TextView)bottomDetailContainer.findViewById(R.id.pn_duration);
        positionBar = (SeekBar)bottomDetailContainer.findViewById(R.id.pn_position_seekbar);
        positionBar.setOnSeekBarChangeListener(seekListener);
        positionBar.setEnabled(false);
        coverArt = (ImageView)view.findViewById(R.id.pn_coverArt);
        coverArt.setOnClickListener(coverArtClickListener);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        repeatItem = menu.findItem(R.id.action_repeat);
        repeatItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(getActivity(), PlaybackService.class);
                intent.setAction(Constants.CMD_TOGGLE_REPEAT_MODE);
                getActivity().startService(intent);
                isRepeatEnabled = !isRepeatEnabled;
                onRepeatToggled();
                return true;
            }
        });

        onRepeatToggled();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().bindService(new Intent(getActivity(), PlaybackService.class), this, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, Utils.createPlaybackIntentFilter());
    }

    @Override
    public void onPause() {
        cancelHideOverlays();
        handler.removeCallbacksAndMessages(null);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        getActivity().unbindService(this);
        super.onPause();
    }

    private void playingItemChanged(Intent intent) {
        handler.removeCallbacksAndMessages(null);
        if (isDetached() || playbackService == null) return;
        if (autoHideOverlays) setOverlaysVisible(true);

        artist.setText(intent.getStringExtra(Constants.EXTRA_ARTIST));
        album.setText(intent.getStringExtra(Constants.EXTRA_ALBUM));
        title.setText(intent.getStringExtra(Constants.EXTRA_TITLE));

        if (!playbackService.isPlaylistEmpty()) {
            playlistPosition.setText(String.valueOf(intent.getIntExtra(Constants.EXTRA_PLAYLIST_POSITION, 0)));
            playlistTracks.setText(String.valueOf(intent.getIntExtra(Constants.EXTRA_PLAYLIST_TOTAL_TRACKS, 0)));
            positionDisplay.setText(R.string.blank_time);

            coverArt.animate().alpha(0.08f).setDuration(75);

            Glide.with(this).load(intent.getStringExtra(Constants.EXTRA_ALBUM_ART_URL)).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                    resetToLogo();
                }

                @Override
                public void onResourceReady(Bitmap art, GlideAnimation<? super Bitmap> glideAnimation) {
                    if (art != null) {
                        Utils.createDropShadowBitmap(art, new Utils.BitmapCallback() {
                            @Override
                            public void onBitmapReady(Bitmap bitmap) {
                                coverArt.setImageBitmap(bitmap);
                                coverArt.animate().alpha(1f).setDuration(175);
                            }
                        });

                        if (getActivity() instanceof PlayingNowActivity)
                            Utils.createBlurredBitmap(art, new Utils.BitmapCallback() {
                                @Override
                                public void onBitmapReady(Bitmap bitmap) {
                                    ((PlayingNowActivity)getActivity()).setBackgroundImage(bitmap);
                                }
                            });
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
    }

    private void playbackStateChanged(Intent intent) {
        handler.removeCallbacksAndMessages(null);
        if (isDetached() || playbackService == null) return;

        PlaybackState state = (PlaybackState)intent.getSerializableExtra(Constants.EXTRA_STATE);
        playPause.setImageResource(R.drawable.play);
        int duration = intent.getIntExtra(Constants.EXTRA_DURATION, -1);

        switch (state) {
            case STOPPED:
                positionDisplay.setText(R.string.blank_time);
                durationDisplay.setText(R.string.blank_time);
                positionBar.setProgress(0);
                positionBar.setEnabled(false);
                playPause.setEnabled(true);
                playPause.setAlpha(1f);
                positionDisplay.clearAnimation();
                break;
            case PLAYING:
                positionBar.setEnabled(true);
                positionBar.setMax(duration);
                playPause.setAlpha(1f);
                playPause.setImageResource(R.drawable.pause);
                playPause.setEnabled(true);
                scheduleHideOverlays();
                positionDisplay.clearAnimation();
                durationDisplay.setText(Utils.longToTimeDisplay(duration));

                updatePosition(intent.getIntExtra(Constants.EXTRA_POSITION, 0));

                handler.postDelayed(new UpdatePositionRunnable(), 1000);

                break;
            case BUFFERING:
                positionBar.setEnabled(false);
                playPause.setEnabled(false);
                playPause.setAlpha(.3f);
                playPause.setImageResource(R.drawable.play);
                positionDisplay.clearAnimation();
                break;
            case PAUSED:
                positionDisplay.setText(Utils.longToTimeDisplay(intent.getIntExtra(Constants.EXTRA_POSITION, 0)));

                positionBar.setMax(duration);
                durationDisplay.setText(Utils.longToTimeDisplay(duration));

                positionDisplay.startAnimation(pauseBlinkAnimation);
                break;
        }

        if (state != PlaybackState.PLAYING) {
            cancelHideOverlays();
            setOverlaysVisible(true);
        }
    }

    private void playlistUpdated() {
        if (isDetached() || playbackService == null) return;

        if (!playbackService.isPlaylistEmpty()) {
            playlistPosition.setText(String.valueOf(playbackService.getPlaylistPositionForDisplay()));
            playlistTracks.setText(String.valueOf(playbackService.getPlaylistSize()));
        } else {
            playlistPosition.setText("0");
            playlistTracks.setText("0");
        }
    }

    private void resetToLogo() {
        coverArt.setAlpha(.1f);
        coverArt.setImageResource(R.drawable.logo);

        if (getActivity() instanceof PlayingNowActivity)
            ((PlayingNowActivity)getActivity()).setBackgroundImage(null);
    }

    private void updatePosition(int position) {
        this.position = position;

        if (!isPositionBarTouched) {
            positionBar.setProgress(position);
            positionBar.setEnabled(true);
            positionDisplay.setText(Utils.longToTimeDisplay(position));
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playbackService = ((PlaybackService.LocalBinder)service).getService();
        playingItemChanged(playbackService.getPlayingItemIntent());
        playbackStateChanged(playbackService.getPlaybackStateIntent());
        this.isRepeatEnabled = playbackService.isRepeatEnabled();
        onRepeatToggled();
    }

    private void onRepeatToggled() {
        if (repeatItem == null) return;
        repeatItem.getIcon().setAlpha(isRepeatEnabled ? 255 : 50);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }

    private void scheduleHideOverlays() {
        if (!autoHideOverlays || playbackService.getState() != PlaybackState.PLAYING) return;

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

    private class UpdatePositionRunnable implements Runnable {
        @Override
        public void run() {
            if (playbackService.getState() == PlaybackState.PLAYING) {
                updatePosition(position + 1000);
                handler.postDelayed(new UpdatePositionRunnable(), 1000);
            }
        }
    }
}
