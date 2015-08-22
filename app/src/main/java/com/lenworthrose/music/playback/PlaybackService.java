package com.lenworthrose.music.playback;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.adapter.PlayingNowPlaylistAdapter;
import com.lenworthrose.music.util.Constants;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * The PlaybackService is responsible for all things playback. It handles scheduling the {@link MediaPlayer}
 * instances that play music based on the current Playing Now Playlist, and provides the interface for modifying
 * the playlist and controlling playback.
 * <p/>
 * The Service is bind-able, which is the preferred way of interacting with it.
 * <p/>
 * It sends out Broadcasts using a {@link LocalBroadcastManager} when playback state or the current item changes.
 * See {@link Constants} for the action strings used.
 */
public class PlaybackService extends Service {
    private final IBinder binder = new LocalBinder();
    private PlaybackThread playbackThread;

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("PlaybackService", "PlaybackService created");

        CountDownLatch latch = new CountDownLatch(1);
        playbackThread = new PlaybackThread(this, latch);
        playbackThread.start();

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Log.e("PlaybackService", "InterruptedException occurred awaiting PlaybackThread initialization", ex);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Constants.CMD_PLAY_PAUSE:
                    playbackThread.getHandler().obtainMessage(PlaybackThread.PLAY_PAUSE).sendToTarget();
                    break;
                case Constants.CMD_STOP:
                    playbackThread.getHandler().obtainMessage(PlaybackThread.STOP).sendToTarget();
                    break;
                case Constants.CMD_PREVIOUS:
                    playbackThread.getHandler().obtainMessage(PlaybackThread.PREVIOUS).sendToTarget();
                    break;
                case Constants.CMD_NEXT:
                    playbackThread.getHandler().obtainMessage(PlaybackThread.NEXT).sendToTarget();
                    break;
                case Constants.CMD_TOGGLE_REPEAT_MODE:
                    playbackThread.getHandler().obtainMessage(PlaybackThread.TOGGLE_REPEAT_MODE).sendToTarget();
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("PlaybackService", "PlaybackService destroyed");
        playbackThread.quit();
        super.onDestroy();
    }

    public boolean isPlaylistEmpty() {
        return playbackThread.getPlaylistSize() == 0;
    }

    public void playPause() {
        playbackThread.getHandler().obtainMessage(PlaybackThread.PLAY_PAUSE).sendToTarget();
    }

    public void next() {
        playbackThread.getHandler().obtainMessage(PlaybackThread.NEXT).sendToTarget();
    }

    public void previous() {
        playbackThread.getHandler().obtainMessage(PlaybackThread.PREVIOUS).sendToTarget();
    }

    public void stop() {
        playbackThread.getHandler().obtainMessage(PlaybackThread.STOP).sendToTarget();
    }

    public void play(int playlistPosition) {
        playbackThread.getHandler().obtainMessage(PlaybackThread.PLAY_FROM_PLAYING_NOW, playlistPosition, 0).sendToTarget();
    }

    public void play(Cursor songsCursor, int from) {
        playbackThread.getHandler().obtainMessage(PlaybackThread.PLAY_CURSOR, from, 0, songsCursor).sendToTarget();
    }

    public void play(IdType type, ArrayList<Long> ids) {
        playbackThread.getHandler().obtainMessage(PlaybackThread.PLAY_LIST, type.ordinal(), 0, ids).sendToTarget();
    }

    public void add(IdType type, ArrayList<Long> ids) {
        playbackThread.getHandler().obtainMessage(PlaybackThread.ADD_LIST, type.ordinal(), 0, ids).sendToTarget();
    }

    public void addAsNext(IdType type, ArrayList<Long> ids) {
        playbackThread.getHandler().obtainMessage(PlaybackThread.ADD_LIST_AS_NEXT, type.ordinal(), 0, ids).sendToTarget();
    }

    public void seek(int position) {
        playbackThread.getHandler().obtainMessage(PlaybackThread.SEEK, position, 0).sendToTarget();
    }

    public void performPlaylistActions(PlayingNowPlaylistAdapter adapter) {
        playbackThread.getHandler().obtainMessage(PlaybackThread.PERFORM_PLAYLIST_ACTIONS, adapter).sendToTarget();
    }

    public void shuffleAll() {
        playbackThread.getHandler().obtainMessage(PlaybackThread.SHUFFLE_ALL).sendToTarget();
    }

    public void shuffleRemaining() {
        playbackThread.getHandler().obtainMessage(PlaybackThread.SHUFFLE_REMAINING).sendToTarget();
    }

    public boolean isRepeatEnabled() {
        return playbackThread.isRepeatEnabled();
    }

    public int getPlaylistSize() {
        return playbackThread.getPlaylistSize();
    }

    public Constants.PlaybackState getState() {
        return playbackThread.getPlaybackState();
    }

    public int getPlaylistPosition() {
        return playbackThread.getPlaylistPosition();
    }

    public int getPlaylistPositionForDisplay() {
        return getPlaylistPosition() + 1;
    }

    public Cursor getPlaylist() {
        return playbackThread.getPlaylist();
    }

    public boolean isPlaying() {
        return playbackThread.isPlaying();
    }

    public Intent getPlayingItemIntent() {
        return playbackThread.getPlayingItemIntent();
    }

    public Intent getPlaybackStateIntent() {
        return playbackThread.getPlaybackStateIntent();
    }

    public Equalizer getEqualizer() {
        return playbackThread.getEqualizer();
    }
}
