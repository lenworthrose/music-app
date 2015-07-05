package com.lenworthrose.music.playback;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.adapter.PlayingNowPlaylistAdapter;
import com.lenworthrose.music.adapter.SongsAdapter;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Constants.PlaybackState;

import java.io.IOException;
import java.util.ArrayList;

public class PlaybackThread extends Thread implements Handler.Callback, MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        AudioManager.OnAudioFocusChangeListener, PlaylistStore.InitListener {
    public static final int PLAY_FROM_PLAYING_NOW = 1;
    public static final int PLAY_PAUSE = 2;
    public static final int NEXT = 3;
    public static final int PREVIOUS = 4;
    public static final int STOP = 5;
    public static final int PLAY_LIST = 6;
    public static final int ADD_LIST = 7;
    public static final int ADD_LIST_AS_NEXT = 8;
    public static final int PLAY_CURSOR = 9;
    public static final int SHUFFLE_ALL = 10;
    public static final int SHUFFLE_REMAINING = 11;
    public static final int SEEK = 12;
    public static final int PERFORM_PLAYLIST_ACTIONS = 13;
    public static final int SET_REPEAT_MODE = 14;

    private Handler handler;
    private MediaPlayer currentTrack, nextTrack;
    private BroadcastReceiver noisyReceiver;
    protected Cursor playlistCursor;
    protected int playlistPosition;
    private Constants.RepeatMode repeatMode = Constants.RepeatMode.OFF;
    private LocalBroadcastManager broadcastMan;
    private AudioManager audioMan;
    private MediaSessionManager mediaSessionManager;
    private PlaylistStore playlistStore;
    private PlaybackService playbackService;
    private PlaybackState playbackState;

    private MediaPlayer.OnPreparedListener nextTrackPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            currentTrack.setNextMediaPlayer(nextTrack);
        }
    };

    public PlaybackThread(PlaybackService playbackService) {
        this.playbackService = playbackService;
        playbackState = PlaybackState.STOPPED;
        repeatMode = Constants.RepeatMode.values()[PreferenceManager.getDefaultSharedPreferences(playbackService).getInt(Constants.SETTING_REPEAT_MODE, 0)];

        broadcastMan = LocalBroadcastManager.getInstance(playbackService);
        audioMan = (AudioManager)playbackService.getSystemService(Context.AUDIO_SERVICE);
        playlistStore = new PlaylistStore();
        playlistStore.setListener(this);
        playlistStore.init(playbackService);
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new Handler(Looper.myLooper(), this);

        handler.post(new Runnable() {
            @Override
            public void run() {
                currentTrack = new MediaPlayer();
                nextTrack = new MediaPlayer();

                mediaSessionManager = new MediaSessionManager(playbackService, handler);
                IntentFilter intentFilter = new IntentFilter(Constants.PLAYBACK_STATE_CHANGED);
                intentFilter.addAction(Constants.PLAYING_NOW_CHANGED);
                broadcastMan.registerReceiver(mediaSessionManager, intentFilter);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        playbackService.onPlaybackThreadInitialized();
                    }
                }, 500); //Delay to give PlaylistStore time to init
            }
        });

        Looper.loop();
    }

    public void quit() {
        broadcastMan.unregisterReceiver(mediaSessionManager);
        unregisterNoisyReceiver();

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    currentTrack.release();
                    nextTrack.release();
                } catch (IllegalStateException ex) {
                    Log.w("PlaybackThread", "IllegalStateException occurred attempting to release MediaPlayers in quit()");
                }

                Looper.myLooper().quit();
            }
        });
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case PLAY_PAUSE:
                playPause();
                break;
            case STOP:
                stopPlayback();
                break;
            case NEXT:
                next();
                break;
            case PREVIOUS:
                previous();
                break;
            case PLAY_FROM_PLAYING_NOW:
                play(msg.arg1);
                break;
            case PLAY_LIST:
                play(getTypeFrom(msg.arg1), (ArrayList<Long>)msg.obj);
                break;
            case ADD_LIST:
                add(getTypeFrom(msg.arg1), (ArrayList<Long>)msg.obj);
                break;
            case ADD_LIST_AS_NEXT:
                addAsNext(getTypeFrom(msg.arg1), (ArrayList<Long>)msg.obj);
                break;
            case PLAY_CURSOR:
                play((Cursor)msg.obj, msg.arg1);
                break;
            case SHUFFLE_ALL:
                shuffleAll();
                break;
            case SHUFFLE_REMAINING:
                shuffleRemaining();
                break;
            case SEEK:
                seek(msg.arg1);
                break;
            case PERFORM_PLAYLIST_ACTIONS:
                performPlaylistActions((PlayingNowPlaylistAdapter)msg.obj);
                break;
            case SET_REPEAT_MODE:
                setRepeatMode(Constants.RepeatMode.values()[msg.arg1]);
                break;
        }

        return true;
    }

    private static final IdType[] TYPE_VALUES = IdType.values();

    private static IdType getTypeFrom(int ordinal) {
        return TYPE_VALUES[ordinal];
    }

    private void playPause() {
        if (!isPlaying()) {
            play(playlistPosition);
        } else {
            if (currentTrack.isPlaying())
                pause();
            else if (currentTrack.getCurrentPosition() > 0)
                playFromPause();
            // else do nothing; track must be buffering, mucking with it will cause an error!
        }
    }

    private void next() {
        if (!isPlaying()) {
            if (!isEndOfPlaylist()) {
                playlistPosition++;
                storePlaylistPosition();
                notifyPlayingItemChanged();
            }
        } else if (!isEndOfPlaylist()) {
            play(playlistPosition + 1);
        }
    }

    private void previous() {
        if (!isPlaying()) {
            if (playlistPosition > 0) {
                playlistPosition--;
                storePlaylistPosition();
                notifyPlayingItemChanged();
            }
        } else {
            if (playlistPosition == 0 || (currentTrack.isPlaying() && currentTrack.getCurrentPosition() > 3000))
                seek(0);
            else if (playlistPosition > 0)
                play(playlistPosition - 1);
        }
    }

    private void stopPlayback() {
        releaseMediaPlayers(true);
    }

    private void seek(int position) {
        if (isPlaying()) {
            try {
                currentTrack.seekTo(position);
                notifyStateChanged(PlaybackState.BUFFERING);
            } catch (IllegalStateException ex) {
                Log.e("PlaybackThread", "Error seeking.", ex);
            }
        }
    }

    private void play(int playlistPosition) {
        if (playlistCursor == null || playlistPosition >= playlistCursor.getCount() || playlistPosition < 0) return;

        releaseMediaPlayers(false);

        this.playlistPosition = playlistPosition;
        playlistCursor.moveToPosition(playlistPosition);
        storePlaylistPosition();

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (audioMan.requestAudioFocus(PlaybackThread.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        currentTrack.setWakeMode(playbackService, PowerManager.PARTIAL_WAKE_LOCK);
                        currentTrack.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        listenOn(currentTrack);
                        currentTrack.setDataSource(playbackService, getUriFromCursor(playlistCursor));
                        currentTrack.prepareAsync();
                        playbackState = PlaybackState.BUFFERING;

                        registerNoisyReceiver();
                        if (mediaSessionManager != null) mediaSessionManager.register();
                    } else {
                        Log.e("PlaybackThread", "Unable to start device playback - AudioManager wouldn't allow us focus!");
                        //TODO: Show some sort of error dialog/toast
                    }

                    notifyPlayingItemChanged();
                } catch (IOException ex) {
                    Log.e("PlaybackThread", "IOException attempting to start playback.", ex);
                }
            }
        });
    }

    private void play(Cursor songsCursor, int from) {
        if (songsCursor == null) return;
        if (from > songsCursor.getCount() || from < 0) from = 0;

        playlistStore.setPlaylist(songsCursor);
        if (playlistCursor != null) playlistCursor.close();
        playlistCursor = playlistStore.read();

        notifyPlaylistChanged();
        play(from);
        notifyPlaybackModificationComplete(Constants.EXTRA_MODIFICATION_TYPE_PLAY);
    }

    private void play(IdType type, ArrayList<Long> ids) {
        playlistStore.setPlaylist(SongsAdapter.createSongsCursor(playbackService, type, ids.get(0)));

        for (int i = 1; i < ids.size(); i++)
            playlistStore.add(SongsAdapter.createSongsCursor(playbackService, type, ids.get(i)));

        if (playlistCursor != null) playlistCursor.close();
        playlistCursor = playlistStore.read();
        notifyPlaylistChanged();
        play(0);
        notifyPlaybackModificationComplete(Constants.EXTRA_MODIFICATION_TYPE_PLAY);
    }

    private void add(final IdType type, final ArrayList<Long> ids) {
        boolean isNextTrackScheduleRequired = isEndOfPlaylist();

        for (Long id : ids)
            playlistStore.add(SongsAdapter.createSongsCursor(playbackService, type, id));

        if (playlistCursor != null) playlistCursor.close();
        playlistCursor = playlistStore.read();

        if (isNextTrackScheduleRequired) {
            cancelNextTrack(); //Just in case!
            scheduleNextTrack();
        }

        notifyPlaylistChanged();
        notifyPlaybackModificationComplete(Constants.EXTRA_MODIFICATION_TYPE_ADD);
    }

    private void addAsNext(final IdType type, final ArrayList<Long> ids) {
        if (isEndOfPlaylist()) {
            add(type, ids);
            return;
        }

        for (int i = ids.size() - 1; i >= 0; i--) {
            long id = ids.get(i);
            playlistStore.addAfter(playlistPosition + 1, SongsAdapter.createSongsCursor(playbackService, type, id));
        }

        if (playlistCursor != null) playlistCursor.close();
        playlistCursor = playlistStore.read();
        cancelNextTrack();
        scheduleNextTrack();
        notifyPlaylistChanged();
        notifyPlaybackModificationComplete(Constants.EXTRA_MODIFICATION_TYPE_ADD_AS_NEXT);
    }

    private void shuffleAll() {
        if (playlistCursor == null || playlistCursor.getCount() == 0) return;

        boolean wasPlaying = isPlaying();
        stopPlayback();

        playlistStore.shuffle(-1);
        if (playlistCursor != null) playlistCursor.close();
        playlistCursor = playlistStore.read();

        notifyPlaylistChanged();

        if (wasPlaying) {
            play(0);
        } else {
            playlistPosition = 0;
            storePlaylistPosition();
            notifyPlayingItemChanged();
        }
    }

    private void shuffleRemaining() {
        if (playlistCursor == null || playlistCursor.getCount() == 0) return;

        playlistStore.shuffle(playlistPosition);
        if (playlistCursor != null) playlistCursor.close();
        playlistCursor = playlistStore.read();

        notifyPlaylistChanged();

        cancelNextTrack();
        scheduleNextTrack();
    }

    public boolean isPlaying() {
        return playbackState == PlaybackState.PLAYING;
    }

    private boolean isEndOfPlaylist() {
        return playlistPosition >= playlistCursor.getCount() - 1;
    }

    public int getDuration() {
        try {
            return playbackState == PlaybackState.PLAYING ? currentTrack.getDuration() : -1;
        } catch (IllegalStateException ex) {
            return -1;
        }
    }

    public Constants.PlaybackState getPlaybackState() {
        return playbackState;
    }

    public int getPosition() {
        try {
            return !isPlaying() ? 0 : currentTrack.getCurrentPosition();
        } catch (IllegalStateException ex) {
            return 0;
        }
    }

    public Cursor getPlaylist() {
        return playlistCursor;
    }

    public int getPlaylistSize() {
        return playlistCursor == null ? 0 : playlistCursor.getCount();
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public Constants.RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(Constants.RepeatMode mode) {
        repeatMode = mode;

        if (isEndOfPlaylist()) {
            if (mode == Constants.RepeatMode.PLAYLIST || mode == Constants.RepeatMode.TRACK) {
                if (playlistCursor != null && playlistCursor.getCount() > 0 && isPlaying() && !isNextTrackPlaying()) scheduleNextTrack();
            } else {
                cancelNextTrack();
            }
        }

        PreferenceManager.getDefaultSharedPreferences(playbackService).edit().putInt(Constants.SETTING_REPEAT_MODE, mode.ordinal()).commit();
    }

    private boolean isNextTrackPlaying() {
        try {
            return nextTrack.isPlaying();
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    private void performPlaylistActions(PlayingNowPlaylistAdapter adapter) {
        Cursor cursor = adapter.getCursor();
        cursor.moveToPosition(playlistPosition);

        long currentSongId = adapter.getCursor().getLong(2);
        playlistStore.performPlaylistActions(adapter);

        if (playlistCursor != null) playlistCursor.close();
        playlistCursor = playlistStore.read();
        playlistPosition = 0;

        int position = playlistStore.findPosition(currentSongId);

        if (position >= 0)
            playlistPosition = position;
        else
            stopPlayback();

        storePlaylistPosition();
        notifyPlaylistChanged();
        notifyPlayingItemChanged();

        cancelNextTrack();
        scheduleNextTrack();
    }

    private void scheduleNextTrack() {
        if (!isPlaying()) return;

        int nextTrackIndex;

        if (isEndOfPlaylist()) {
            if (repeatMode == Constants.RepeatMode.OFF || repeatMode == Constants.RepeatMode.STOP
                    || playlistCursor == null || playlistCursor.getCount() == 0)
                return;

            nextTrackIndex = 0;
        } else {
            nextTrackIndex = playlistPosition + 1;
        }

        playlistCursor.moveToPosition(nextTrackIndex);

        try {
            nextTrack.setWakeMode(playbackService, PowerManager.PARTIAL_WAKE_LOCK);
            nextTrack.setAudioStreamType(AudioManager.STREAM_MUSIC);
            nextTrack.setDataSource(playbackService, getUriFromCursor(playlistCursor));
            nextTrack.setOnPreparedListener(nextTrackPreparedListener);
            nextTrack.prepareAsync();
        } catch (IOException ex) {
            Log.e("PlaybackThread", "Error setting data source for next track.", ex);
        }
    }

    private void pause() {
        currentTrack.pause();
        notifyStateChanged(PlaybackState.PAUSED);
    }

    private void playFromPause() {
        currentTrack.start();
        notifyStateChanged(PlaybackState.PLAYING);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("PlaybackThread", String.format("Media player error: %d %d %s", what, extra, playbackState));

        if (playbackState != PlaybackState.STOPPED) {
            releaseMediaPlayers(true);
            //TODO: Show an error message/dialog/toast
        }

        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i("PlaybackThread", "Playback complete!");

        if (nextTrack != null) {
            currentTrack.reset();

            playlistPosition = isEndOfPlaylist() ? 0 : playlistPosition + 1; //If next track isn't null and we're at playlist end, RepeatMode must be Playlist. Start at 0!
            storePlaylistPosition();
            currentTrack = nextTrack;
            listenOn(currentTrack);

            notifyPlayingItemChanged();
            notifyStateChanged(PlaybackState.PLAYING);

            scheduleNextTrack();
        } else {
            releaseMediaPlayers(true);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.i("PlaybackThread", "Media player info: " + what);

//        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) || (what == MediaPlayer.MEDIA_INFO_BUFFERING_END && mp.isPlaying()))
//            notifyStateChanged();

        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        currentTrack.start();
        notifyStateChanged(PlaybackState.PLAYING);
        scheduleNextTrack();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        notifyStateChanged(PlaybackState.PLAYING);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (isPlaying()) currentTrack.setVolume(1.0f, 1.0f);
                // Do nothing...?
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                releaseMediaPlayers(true);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (isPlaying()) currentTrack.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private void cancelNextTrack() {
        if (nextTrack != null) {
            currentTrack.setNextMediaPlayer(null);
            nextTrack.reset();
        }
    }

    /**
     * @param shouldUnregisterRemote should be true if we aren't starting playback again. Otherwise, lockscreen widget will flicker!
     */
    private void releaseMediaPlayers(boolean shouldUnregisterRemote) {
        try {
            nextTrack.reset();
            currentTrack.reset();
        } catch (IllegalStateException ex) {
            Log.w("PlaybackThread", "IllegalStateException occurred attempting to reset MediaPlayers");
        }

        if (shouldUnregisterRemote) {
            if (mediaSessionManager != null) mediaSessionManager.unregister();
            audioMan.abandonAudioFocus(this);
        }

        unregisterNoisyReceiver();
        notifyStateChanged(PlaybackState.STOPPED);
    }

    private void registerNoisyReceiver() {
        if (noisyReceiver == null) {
            noisyReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY))
                        if (isPlaying())
                            pause();
                }
            };

            playbackService.registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }
    }

    private void unregisterNoisyReceiver() {
        if (noisyReceiver != null) {
            playbackService.unregisterReceiver(noisyReceiver);
            noisyReceiver = null;
        }
    }

    private void listenOn(MediaPlayer player) {
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnInfoListener(this);
        player.setOnSeekCompleteListener(this);
    }

    protected int getStoredPlaylistPosition() {
        return PreferenceManager.getDefaultSharedPreferences(playbackService).getInt("DevicePlaylistPosition", 0);
    }

    protected void storePlaylistPosition() {
        PreferenceManager.getDefaultSharedPreferences(playbackService).edit().putInt("DevicePlaylistPosition", playlistPosition).apply();
    }

    private void notifyPlayingItemChanged() {
        broadcastMan.sendBroadcast(getPlayingItemIntent());
    }

    public Intent getPlayingItemIntent() {
        PlayingItem item = new PlayingItem(playlistCursor, playlistPosition);

        Intent intent = new Intent(Constants.PLAYING_NOW_CHANGED);
        intent.putExtra(Constants.EXTRA_ARTIST, item.getArtist());
        intent.putExtra(Constants.EXTRA_ALBUM, item.getAlbum());
        intent.putExtra(Constants.EXTRA_TITLE, item.getTitle());
        intent.putExtra(Constants.EXTRA_TRACK_NUM, item.getTrackNum());
        intent.putExtra(Constants.EXTRA_PLAYLIST_POSITION, playlistPosition + 1);
        intent.putExtra(Constants.EXTRA_PLAYLIST_TOTAL_TRACKS, getPlaylistSize());
        intent.putExtra(Constants.EXTRA_ALBUM_ART_URL, item.getAlbumArtUrl());
        intent.putExtra(Constants.EXTRA_DURATION, item.getDuration());

        return intent;
    }

    public Intent getPlaybackStateIntent() {
        Intent intent = new Intent(Constants.PLAYBACK_STATE_CHANGED);
        intent.putExtra(Constants.EXTRA_STATE, playbackState);
        if (playbackState == PlaybackState.PLAYING) intent.putExtra(Constants.EXTRA_DURATION, getDuration());
        return intent;
    }

    private void notifyPlaylistChanged() {
        broadcastMan.sendBroadcast(new Intent(Constants.PLAYING_NOW_PLAYLIST_CHANGED));
    }

    private void notifyStateChanged(PlaybackState state) {
        playbackState = state;
        broadcastMan.sendBroadcast(getPlaybackStateIntent());
    }

    private void notifyPlaybackModificationComplete(int which) {
        Intent intent = new Intent(Constants.PLAYBACK_MODIFICATION_COMPLETE);
        intent.putExtra(Constants.EXTRA_MODIFICATION_TYPE, which);
        broadcastMan.sendBroadcast(intent);
    }

    private Uri getUriFromCursor(Cursor cursor) {
        return getUriForMedia(cursor.getLong(2)); //TODO: undo hardcode!
    }

    private Uri getUriForMedia(long id) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

    @Override
    public void onInitComplete() {
        playlistCursor = playlistStore.read();
        playlistPosition = getStoredPlaylistPosition();
        if (playlistPosition >= playlistCursor.getCount()) playlistPosition = 0;

        notifyPlaylistChanged();
        notifyPlayingItemChanged();
    }
}
