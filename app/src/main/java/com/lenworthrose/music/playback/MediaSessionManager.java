package com.lenworthrose.music.playback;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.lenworthrose.music.R;
import com.lenworthrose.music.activity.PlayingNowActivity;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Utils;

/**
 * The MediaSessionManager listens on {@link PlaybackService} Broadcasts, pushing
 * metadata changes to {@link MediaSessionCompat} and a {@link Notification}. It
 * also contains {@link com.lenworthrose.music.playback.MediaSessionManager.MediaKeyReceiver},
 * which is responsible for listening on media key presses.
 */
public class MediaSessionManager extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 666;

    private MediaSessionCompat mediaSession;
    private PlaybackService playbackService;
    private Intent lastPlayingItemChangedIntent;
    private Handler bgHandler;

    public static class MediaKeyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event == null || event.getAction() == KeyEvent.ACTION_UP) return;

                String action = null;

                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        action = Constants.CMD_PLAY_PAUSE;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        action = Constants.CMD_NEXT;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        action = Constants.CMD_PREVIOUS;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        action = Constants.CMD_STOP;
                        break;
                }

                if (action != null) {
                    Intent serviceIntent = new Intent(context, PlaybackService.class);
                    serviceIntent.setAction(action);
                    context.startService(serviceIntent);
                }
            }
        }
    }

    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
        private Handler handler = new Handler();

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonEvent.getAction())) {
                KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                if (event != null && event.getAction() != KeyEvent.ACTION_UP) {
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            onPlay();
                            return true;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            onSkipToNext();
                            return true;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            onSkipToPrevious();
                            return true;
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            onStop();
                            return true;
                    }
                }
            }

            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {
            playbackService.playPause();
        }

        @Override
        public void onPause() {
            playbackService.playPause();
        }

        @Override
        public void onSkipToNext() {
            playbackService.next();
        }

        @Override
        public void onSkipToPrevious() {
            playbackService.previous();
        }

        @Override
        public void onSeekTo(long pos) {
            final int intPos = (int)pos;
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playbackService.seek(intPos);
                }
            }, 400);
        }

        @Override
        public void onStop() {
            playbackService.stop();
        }
    };

    public MediaSessionManager(PlaybackService playbackService, Handler bgHandler) {
        this.playbackService = playbackService;
        this.bgHandler = bgHandler;
    }
    
    @SuppressWarnings("deprecation")
    public void onStateChanged(Intent intent) {
        if (mediaSession == null) return;

        Constants.PlaybackState newState = (Constants.PlaybackState)intent.getSerializableExtra(Constants.EXTRA_STATE);

        if (newState == Constants.PlaybackState.STOPPED) {
            playbackService.stopForeground(true);
            mediaSession.setActive(false);
            return;
        }

        int state, position;
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SEEK_TO;

        switch (newState) {
            case BUFFERING:
                state = PlaybackStateCompat.STATE_BUFFERING;
                position = 0;
                break;
            case PLAYING:
                state = PlaybackStateCompat.STATE_PLAYING;
                position = playbackService.getPosition();
                break;
            case PAUSED:
                state = PlaybackStateCompat.STATE_PAUSED;
                position = playbackService.getPosition();
                break;
            default:
                state = PlaybackStateCompat.STATE_NONE;
                position = 0;
                break;
        }

        try {
            PlaybackStateCompat.Builder pb = new PlaybackStateCompat.Builder();
            pb.setState(state, position, 1.0f);
            pb.setActions(actions);
            mediaSession.setPlaybackState(pb.build());
            mediaSession.setActive(true);

            /**
             * Since the PlaybackStateCompat's actions aren't really used, we need to jam the transport control flags
             * into the RemoteControlClient manually.
             */
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                        | RemoteControlClient.FLAG_KEY_MEDIA_NEXT | RemoteControlClient.FLAG_KEY_MEDIA_STOP;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) flags |= RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE;

                ((RemoteControlClient)mediaSession.getRemoteControlClient()).setTransportControlFlags(flags);
            }
        } catch (Exception ex) {
            Log.e("MediaSessionManager", ex.getClass().getName() + " occurred in onStateChanged: " + ex.getMessage(), ex);
        }
    }

    private void updateMetadata(Intent intent, Bitmap art) {
        MediaMetadataCompat.Builder b = new MediaMetadataCompat.Builder();
        b.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, intent.getStringExtra(Constants.EXTRA_ARTIST));
        b.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, intent.getStringExtra(Constants.EXTRA_ARTIST));
        b.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, intent.getStringExtra(Constants.EXTRA_ALBUM));
        b.putString(MediaMetadataCompat.METADATA_KEY_TITLE, intent.getStringExtra(Constants.EXTRA_TITLE));
        b.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);
        b.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, intent.getLongExtra(Constants.EXTRA_DURATION, 0));

        int trackNum = intent.getIntExtra(Constants.EXTRA_TRACK_NUM, -1);

        if (trackNum != -1) { b.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNum); }

        try {
            mediaSession.setMetadata(b.build());
            mediaSession.setActive(true);
        } catch (Exception ex) {
            Log.e("MediaSessionManager", ex.getClass().getName() + " occurred in onPlayingItemChanged: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        switch (intent.getAction()) {
            case Constants.PLAYBACK_STATE_CHANGED:
                bgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onStateChanged(intent);
                    }
                });

                if (!playbackService.isPlaylistEmpty()) { //Update the Notification, esp. to switch Play/Pause icons
                    Glide.with(playbackService).load(lastPlayingItemChangedIntent.getStringExtra(Constants.EXTRA_ALBUM_ART_URL)).asBitmap().into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            bgHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mediaSession == null) return;
                                    updateNotification(lastPlayingItemChangedIntent, BitmapFactory.decodeResource(playbackService.getResources(), R.drawable.logo));
                                }
                            });
                        }

                        @Override
                        public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            bgHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mediaSession == null) return;
                                    updateNotification(lastPlayingItemChangedIntent, resource);
                                }
                            });
                        }
                    });
                }

                break;
            case Constants.PLAYING_NOW_CHANGED:
                lastPlayingItemChangedIntent = intent;

                Glide.with(playbackService).load(intent.getStringExtra(Constants.EXTRA_ALBUM_ART_URL)).asBitmap().into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        Bitmap fallback = BitmapFactory.decodeResource(playbackService.getResources(), R.drawable.audio);
                        if (mediaSession == null) return;
                        updateMetadata(lastPlayingItemChangedIntent, fallback);
                        updateNotification(lastPlayingItemChangedIntent, fallback);
                    }

                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        bgHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mediaSession == null) return;
                                updateMetadata(lastPlayingItemChangedIntent, resource);
                                updateNotification(lastPlayingItemChangedIntent, resource);
                            }
                        });
                    }
                });

                break;
        }
    }

    public void register() {
        if (mediaSession == null) {
            ComponentName component = new ComponentName(playbackService, MediaKeyReceiver.class);
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setComponent(component);
            PendingIntent mediaButton = PendingIntent.getBroadcast(playbackService, 0, mediaButtonIntent, 0);

            Intent launchIntent = new Intent(playbackService, PlayingNowActivity.class);
            PendingIntent launch = PendingIntent.getActivity(playbackService, 666, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            mediaSession = new MediaSessionCompat(playbackService, "muZak", component, mediaButton);
            mediaSession.setCallback(mediaSessionCallback);
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setSessionActivity(launch);
        }
    }

    public void unregister() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
    }

    public MediaSessionCompat.Token getSessionToken() {
        return mediaSession == null ? null : mediaSession.getSessionToken();
    }

    private void updateNotification(Intent intent, Bitmap coverArt) {
        String ticker;
        StringBuilder sb = new StringBuilder(intent.getStringExtra(Constants.EXTRA_TITLE));
        String artist = intent.getStringExtra(Constants.EXTRA_ARTIST);

        if (!TextUtils.isEmpty(artist)) sb.append(" - ").append(artist);
        ticker = sb.toString();

        sb = new StringBuilder();
        if (!artist.isEmpty()) sb.append(artist).append(" - ");
        sb.append(intent.getStringExtra(Constants.EXTRA_ALBUM));

        PendingIntent pi = Utils.createPlayingNowPendingIntentWithBackstack(playbackService, 665);
        Notification.Builder builder = new Notification.Builder(playbackService);
        builder.setTicker(ticker).setSmallIcon(R.drawable.audio).setOngoing(true).setLargeIcon(coverArt).setContentIntent(pi)
                .setContentTitle(intent.getStringExtra(Constants.EXTRA_TITLE)).setContentText(sb);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getSessionToken() != null) {
            Notification.MediaStyle style = new Notification.MediaStyle();
            style.setMediaSession((MediaSession.Token)getSessionToken().getToken());
            builder.setStyle(style);
        }

        builder.addAction(R.drawable.skip_previous, playbackService.getString(R.string.previous), createMediaPendingIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS));

        Constants.PlaybackState state = playbackService.getState();
        int drawable = state == Constants.PlaybackState.PAUSED ? R.drawable.play : R.drawable.pause;
        String text = playbackService.getString(state == Constants.PlaybackState.PAUSED ? R.string.play : R.string.pause);
        builder.addAction(drawable, text, createMediaPendingIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));

        builder.addAction(R.drawable.skip_next, playbackService.getString(R.string.next), createMediaPendingIntent(KeyEvent.KEYCODE_MEDIA_NEXT));
        playbackService.startForeground(NOTIFICATION_ID, builder.build());
    }

    private PendingIntent createMediaPendingIntent(int keycode) {
        Intent intent = new Intent(playbackService, MediaKeyReceiver.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keycode));
        return PendingIntent.getBroadcast(playbackService, keycode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}

