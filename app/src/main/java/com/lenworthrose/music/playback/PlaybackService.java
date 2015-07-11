package com.lenworthrose.music.playback;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.adapter.PlayingNowPlaylistAdapter;
import com.lenworthrose.music.util.Constants;

import java.util.ArrayList;

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
    private Handler handler;
    private int activityCount = 0;
    private int pendingCommand;

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
        handler = new Handler(Looper.getMainLooper());
        playbackThread = new PlaybackThread(this);
        playbackThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Constants.CMD_PLAY_PAUSE:
                    if (playbackThread.getHandler() != null)
                        playbackThread.getHandler().obtainMessage(PlaybackThread.PLAY_PAUSE).sendToTarget();
                    else
                        pendingCommand = PlaybackThread.PLAY_PAUSE;

                    break;
                case Constants.CMD_STOP:
                    if (playbackThread.getHandler() != null)
                        playbackThread.getHandler().obtainMessage(PlaybackThread.STOP).sendToTarget();
                    else
                        pendingCommand = PlaybackThread.STOP;

                    break;
                case Constants.CMD_PREVIOUS:
                    if (playbackThread.getHandler() != null)
                        playbackThread.getHandler().obtainMessage(PlaybackThread.PREVIOUS).sendToTarget();
                    else
                        pendingCommand = PlaybackThread.PREVIOUS;

                    break;
                case Constants.CMD_NEXT:
                    if (playbackThread.getHandler() != null)
                        playbackThread.getHandler().obtainMessage(PlaybackThread.NEXT).sendToTarget();
                    else
                        pendingCommand = PlaybackThread.NEXT;

                    break;
                case Constants.CMD_ACTIVITY_STARTING:
                    activityCount++;
                    handler.removeCallbacksAndMessages(null);
                    break;
                case Constants.CMD_ACTIVITY_CLOSING:
                    activityCount--;

                    if (activityCount <= 0) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                stopSelf();
                            }
                        }, 750);
                    }

                    break;
                case Constants.CMD_TOGGLE_REPEAT_MODE:
                    if (playbackThread.getHandler() != null)
                        playbackThread.getHandler().obtainMessage(PlaybackThread.TOGGLE_REPEAT_MODE).sendToTarget();
                    else
                        pendingCommand = PlaybackThread.TOGGLE_REPEAT_MODE;
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
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

    void onPlaybackThreadInitialized() {
        if (pendingCommand != 0) playbackThread.getHandler().obtainMessage(pendingCommand).sendToTarget();
    }
}
