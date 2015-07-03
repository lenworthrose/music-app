package com.lenworthrose.music.playback;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.adapter.PlayingNowPlaylistAdapter;
import com.lenworthrose.music.adapter.SongsAdapter;
import com.lenworthrose.music.util.Constants;

import java.io.IOException;
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
public class PlaybackService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        AudioManager.OnAudioFocusChangeListener, PlaylistStore.InitListener {
    public interface PlaybackModificationCompleteListener {
        void onPlaybackModificationComplete();
    }

    private PlaylistStore playlistStore;
    private MediaPlayer currentTrack;
    private MediaPlayer nextTrack;
    private boolean isPrepared;
    private WifiManager.WifiLock wifiLock;
    private AudioManager audioMan;
    private BroadcastReceiver noisyReceiver;
    private MediaSessionManager mediaSessionManager;
    protected Cursor playlistCursor;
    protected int playlistPosition;
    private Constants.RepeatMode repeatMode = Constants.RepeatMode.OFF;
    private LocalBroadcastManager broadcastMan;
    private final IBinder binder = new LocalBinder();

    private MediaPlayer.OnPreparedListener nextTrackPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            currentTrack.setNextMediaPlayer(nextTrack);
        }
    };

    @Override
    public void onInitComplete() {
        playlistCursor = playlistStore.read();
        playlistPosition = getStoredPlaylistPosition();
        if (playlistPosition >= playlistCursor.getCount()) playlistPosition = 0;

        notifyPlaylistChanged();
        notifyPlayingItemChanged();
    }

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
        broadcastMan = LocalBroadcastManager.getInstance(this);
        audioMan = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        playlistStore = new PlaylistStore();
        playlistStore.setListener(this);
        playlistStore.init(this);

        mediaSessionManager = new MediaSessionManager(this);
        IntentFilter intentFilter = new IntentFilter(Constants.PLAYBACK_STATE_CHANGED);
        intentFilter.addAction(Constants.PLAYING_NOW_CHANGED);
        broadcastMan.registerReceiver(mediaSessionManager, intentFilter);

        repeatMode = Constants.RepeatMode.values()[PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.SETTING_REPEAT_MODE, 0)];
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Constants.CMD_PLAY_PAUSE:
                    playPause();
                    break;
                case Constants.CMD_STOP:
                    stop();
                    break;
                case Constants.CMD_PREVIOUS:
                    previous();
                    break;
                case Constants.CMD_NEXT:
                    next();
                    break;
            }
        }

        return START_STICKY;
    }

    public void playPause() {
        if (currentTrack == null) {
            play(playlistPosition);
        } else {
            if (currentTrack.isPlaying())
                pause();
            else if (currentTrack.getCurrentPosition() > 0)
                playFromPause();
            // else do nothing; track must be buffering, mucking with it will cause an error!
        }
    }

    public void next() {
        if (currentTrack == null) {
            if (!isEndOfPlaylist()) {
                playlistPosition++;
                storePlaylistPosition();
                notifyPlayingItemChanged();
            }
        } else if (!isEndOfPlaylist()) {
            play(playlistPosition + 1);
        }
    }

    public void previous() {
        if (currentTrack == null) {
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

    public void stop() {
        releaseMediaPlayers(true);
    }

    public boolean isPlaying() {
        return currentTrack != null && currentTrack.isPlaying();
    }

    public void seek(int position) {
        if (isPlaying()) {
            try {
                currentTrack.seekTo(position);
                notifyStateChanged();
            } catch (IllegalStateException ex) {
                Log.e("DeviceMediaManager", "Error seeking.", ex);
            }
        }
    }

    public int getDuration() {
        try {
            return currentTrack != null && isPrepared ? currentTrack.getDuration() : -1;
        } catch (IllegalStateException ex) {
            return -1;
        }
    }

    public Constants.PlaybackState getState() {
        if (currentTrack != null) {
            if (currentTrack.isPlaying())
                return Constants.PlaybackState.PLAYING;
            else if (currentTrack.getCurrentPosition() > 0)
                return Constants.PlaybackState.PAUSED;
            else
                return Constants.PlaybackState.BUFFERING;
        }

        return Constants.PlaybackState.STOPPED;
    }

    public int getPosition() {
        try {
            return currentTrack == null || !isPrepared ? 0 : currentTrack.getCurrentPosition();
        } catch (IllegalStateException ex) {
            return 0;
        }
    }

    public void setRepeatMode(Constants.RepeatMode mode) {
        repeatMode = mode;

        if (isEndOfPlaylist()) {
            if (mode == Constants.RepeatMode.PLAYLIST || mode == Constants.RepeatMode.TRACK) {
                if (playlistCursor != null && playlistCursor.getCount() > 0 && currentTrack != null && nextTrack == null) scheduleNextTrack();
            } else {
                cancelNextTrack();
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(Constants.SETTING_REPEAT_MODE, mode.ordinal()).commit();
    }

    public void play(int playlistPosition) {
        try {
            if (playlistCursor == null || playlistPosition >= playlistCursor.getCount() || playlistPosition < 0) return;

            this.playlistPosition = playlistPosition;
            playlistCursor.moveToPosition(playlistPosition);
            storePlaylistPosition();

            releaseMediaPlayers(false);

            if (audioMan.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                currentTrack = new MediaPlayer();
                currentTrack.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
                currentTrack.setAudioStreamType(AudioManager.STREAM_MUSIC);
                listenOn(currentTrack);
                currentTrack.setDataSource(this, getUriFromCursor(playlistCursor));
                currentTrack.prepareAsync();

                acquireWifiLock();
                registerNoisyReceiver();
                if (mediaSessionManager != null) mediaSessionManager.register();

                notifyPlayingItemChanged();
                notifyStateChanged();

                return;
            } else {
                Log.e("DeviceMediaManager", "Unable to start device playback - AudioManager wouldn't allow us focus!");
                //TODO: Show some sort of error dialog/toast
            }

            notifyPlayingItemChanged();
        } catch (IOException ex) {
            Log.e("DeviceMediaManager", "IOException attempting to start playback.", ex);
        }
    }

    public void play(Cursor songsCursor, final int from, final PlaybackModificationCompleteListener listener) {
        if (songsCursor == null) return;

        AsyncTask<Cursor, Void, Integer> playTask = new AsyncTask<Cursor, Void, Integer>() {
            @Override
            protected Integer doInBackground(Cursor... params) {
                Cursor songsCursor = params[0];

                int retVal = (from > songsCursor.getCount() || from < 0) ? 0 : from;

                playlistStore.setPlaylist(songsCursor);
                if (playlistCursor != null) playlistCursor.close();
                playlistCursor = playlistStore.read();
                return retVal;
            }

            @Override
            protected void onPostExecute(Integer fromPos) {
                notifyPlaylistChanged();
                play(fromPos);
                listener.onPlaybackModificationComplete();
            }
        };

        playTask.execute(songsCursor);
    }

    public void play(final IdType type, final ArrayList<Long> ids, final PlaybackModificationCompleteListener listener) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                playlistStore.setPlaylist(SongsAdapter.createSongsCursor(PlaybackService.this, type, ids.get(0)));

                for (int i = 1; i < ids.size(); i++)
                    playlistStore.add(SongsAdapter.createSongsCursor(PlaybackService.this, type, ids.get(i)));

                if (playlistCursor != null) playlistCursor.close();
                playlistCursor = playlistStore.read();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                notifyPlaylistChanged();
                play(0);
                listener.onPlaybackModificationComplete();
            }
        };

        task.execute();
    }

    public void add(final IdType type, final ArrayList<Long> ids, final PlaybackModificationCompleteListener listener) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                boolean retVal = isEndOfPlaylist();

                for (Long id : ids)
                    playlistStore.add(SongsAdapter.createSongsCursor(PlaybackService.this, type, id));

                if (playlistCursor != null) playlistCursor.close();
                playlistCursor = playlistStore.read();

                return retVal;
            }

            @Override
            protected void onPostExecute(Boolean isNextTrackScheduleRequired) {
                if (isNextTrackScheduleRequired) {
                    cancelNextTrack(); //Just in case!
                    scheduleNextTrack();
                }

                notifyPlaylistChanged();
                listener.onPlaybackModificationComplete();
            }
        };

        task.execute();
    }

    public void addAsNext(final IdType type, final ArrayList<Long> ids, final PlaybackModificationCompleteListener listener) {
        if (isEndOfPlaylist()) {
            add(type, ids, listener);
            return;
        }

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                for (int i = ids.size() - 1; i >= 0; i--) {
                    long id = ids.get(i);
                    playlistStore.addAfter(playlistPosition + 1, SongsAdapter.createSongsCursor(PlaybackService.this, type, id));
                }

                if (playlistCursor != null) playlistCursor.close();
                playlistCursor = playlistStore.read();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                cancelNextTrack();
                scheduleNextTrack();
                notifyPlaylistChanged();
                listener.onPlaybackModificationComplete();
            }
        };

        task.execute();
    }

    public int getPlaylistSize() {
        return playlistCursor == null ? 0 : playlistCursor.getCount();
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public int getPlaylistPositionForDisplay() {
        return getPlaylistPosition() + 1;
    }

    public Constants.RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public PlayingItem getPlayingItem() {
        return new PlayingItem(playlistCursor, playlistPosition);
    }

    public Cursor getPlaylist() {
        return playlistCursor;
    }

    public boolean isPlaylistEmpty() {
        return getPlaylistSize() == 0;
    }

    public void shuffleAll() {
        if (playlistCursor == null || playlistCursor.getCount() == 0) return;

        final boolean wasPlaying = isPlaying();
        stop();

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                playlistStore.shuffle(-1);
                if (playlistCursor != null) playlistCursor.close();
                playlistCursor = playlistStore.read();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                notifyPlaylistChanged();

                if (wasPlaying) {
                    play(0);
                } else {
                    playlistPosition = 0;
                    storePlaylistPosition();
                    notifyPlayingItemChanged();
                }
            }
        };

        task.execute();
    }

    public void shuffleRemaining() {
        if (playlistCursor == null || playlistCursor.getCount() == 0) return;

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                playlistStore.shuffle(playlistPosition);
                if (playlistCursor != null) playlistCursor.close();
                playlistCursor = playlistStore.read();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                notifyPlaylistChanged();

                cancelNextTrack();
                scheduleNextTrack();
            }
        };

        task.execute();
    }

    public void performPlaylistActions(PlayingNowPlaylistAdapter adapter) {
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
            stop();

        storePlaylistPosition();
        notifyPlaylistChanged();
        notifyPlayingItemChanged();

        cancelNextTrack();
        scheduleNextTrack();
    }

    private void scheduleNextTrack() {
        if (currentTrack == null) return;

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
            nextTrack = new MediaPlayer();
            nextTrack.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            nextTrack.setAudioStreamType(AudioManager.STREAM_MUSIC);
            nextTrack.setDataSource(this, getUriFromCursor(playlistCursor));
            nextTrack.setOnPreparedListener(nextTrackPreparedListener);
            nextTrack.prepareAsync();
        } catch (IOException ex) {
            Log.e("DeviceMediaManager", "Error setting data source for next track.", ex);
        }
    }

    private void pause() {
        currentTrack.pause();
        notifyStateChanged();
    }

    private void playFromPause() {
        currentTrack.start();
        notifyStateChanged();
    }

    private boolean isEndOfPlaylist() {
        return playlistPosition >= playlistCursor.getCount() - 1;
    }

    private void notifyPlayingItemChanged() {
        broadcastMan.sendBroadcast(new Intent(Constants.PLAYING_NOW_CHANGED));
    }

    private void notifyPlaylistChanged() {
        broadcastMan.sendBroadcast(new Intent(Constants.PLAYING_NOW_PLAYLIST_CHANGED));
    }

    private void notifyStateChanged() {
        broadcastMan.sendBroadcast(new Intent(Constants.PLAYBACK_STATE_CHANGED));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("DeviceMediaManager", String.format("Media player error: %d %d", what, extra));
        releaseMediaPlayers(true);
        //TODO: Show an error message/dialog/toast
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i("DeviceMediaManager", "Playback complete!");

        if (nextTrack != null) {
            currentTrack.release();
            currentTrack = null;

            playlistPosition = isEndOfPlaylist() ? 0 : playlistPosition + 1; //If next track isn't null and we're at playlist end, RepeatMode must be Playlist. Start at 0!
            currentTrack = nextTrack;
            listenOn(currentTrack);
            nextTrack = null;

            notifyPlayingItemChanged();

            scheduleNextTrack();
        } else {
            releaseMediaPlayers(true);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.i("DeviceMediaManager", "Media player info: " + what);

        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START || (what == MediaPlayer.MEDIA_INFO_BUFFERING_END && mp.isPlaying()))
            notifyStateChanged();

        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        mp.start();
        notifyStateChanged();
        scheduleNextTrack();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        notifyStateChanged();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (currentTrack != null) currentTrack.setVolume(1.0f, 1.0f);
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

    protected void cancelNextTrack() {
        if (nextTrack != null) {
            currentTrack.setNextMediaPlayer(null);
            nextTrack.release();
            nextTrack = null;
        }
    }

    /**
     * @param shouldUnregisterRemote should be true if we aren't starting playback again. Otherwise, lockscreen widget will flicker!
     */
    private void releaseMediaPlayers(boolean shouldUnregisterRemote) {
        if (nextTrack != null) {
            nextTrack.release();
            nextTrack = null;
        }

        if (currentTrack != null) {
            currentTrack.release();
            currentTrack = null;
        }

        isPrepared = false;

        if (shouldUnregisterRemote) {
            if (mediaSessionManager != null) mediaSessionManager.unregister();
            audioMan.abandonAudioFocus(this);
            stopForeground(true);
        }

        if (wifiLock != null) {
            if (wifiLock.isHeld()) wifiLock.release();
            wifiLock = null;
        }

        unregisterNoisyReceiver();
        notifyStateChanged();
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

            registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }
    }

    private void unregisterNoisyReceiver() {
        if (noisyReceiver != null) {
            unregisterReceiver(noisyReceiver);
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

    private void acquireWifiLock() {
        if (wifiLock == null) {
            wifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "musicWifiLock");
            wifiLock.acquire();
        }
    }

    protected int getStoredPlaylistPosition() {
        return PreferenceManager.getDefaultSharedPreferences(this).getInt("DevicePlaylistPosition", 0);
    }

    protected void storePlaylistPosition() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("DevicePlaylistPosition", playlistPosition).apply();
    }

    private Uri getUriFromCursor(Cursor cursor) {
        return getUriForMedia(cursor.getLong(2)); //TODO: undo hardcode!
    }

    private Uri getUriForMedia(long id) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }
}
