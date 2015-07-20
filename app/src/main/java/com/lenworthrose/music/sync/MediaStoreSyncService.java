package com.lenworthrose.music.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.lenworthrose.music.R;
import com.lenworthrose.music.util.Constants;

import java.util.List;

import fm.last.api.LastFmServer;
import fm.last.api.LastFmServerFactory;

/**
 * A {@link Service} that listens for changes to the {@link android.provider.MediaStore} database. It will also perform the first
 * sync with the MediaStore database to build the app's own Artist database.
 */
public class MediaStoreSyncService extends Service implements ArtistsStore.InitListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ACTION_MEDIA_STORE_SYNC_COMPLETE = "com.lenworthrose.music.sync.MediaStoreSyncService.SYNC_COMPLETE";
    public static final String ACTION_SYNC_WITH_MEDIA_STORE = "com.lenworthrose.music.sync.MediaStoreSyncService.SYNC";
    public static final String ACTION_UPDATE_ALBUMS = "com.lenworthrose.music.sync.MediaStoreSyncService.UPDATE_ALBUMS";
    public static final String ACTION_LAST_FM_FETCH = "com.lenworthrose.music.sync.MediaStoreSyncService.LAST_FM_FETCH";

    private static final int NOTIFICATION_ID = 36663;
    private static final String SETTING_HAS_COMPLETED_INITIAL_SYNC = "HasCompletedInitialSync";

    private boolean isObservingMediaStore, isTaskActive, isArtistSyncPending, isAlbumUpdatePending;
    private ContentObserver artistsObserver, albumsObserver;
    private NotificationManager notifyMan;
    private Notification.Builder notificationBuilder;
    private LastFmServer lastFm;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifyMan = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new Notification.Builder(this).setSmallIcon(R.drawable.sync);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        if (prefs.getBoolean(Constants.SETTING_LAST_FM_INTEGRATION, false)) lastFm = createLastFmServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_SYNC_WITH_MEDIA_STORE:
                    startArtistsSync();
                    return START_STICKY;
                case ACTION_UPDATE_ALBUMS:
                    startUpdatingAlbums(true);
                    return START_STICKY;
                case ACTION_LAST_FM_FETCH:
                    startLastFmUpdate();
                    return START_STICKY;
            }
        }

        ArtistsStore.getInstance().init(this, this);

        return START_STICKY;
    }

    @Override
    public void onArtistsDbInitialized() {
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SETTING_HAS_COMPLETED_INITIAL_SYNC, false))
            startArtistsSync();
        else
            startObservingMediaStore();
    }

    private void startObservingMediaStore() {
        if (!isObservingMediaStore) {
            isObservingMediaStore = true;

            artistsObserver = new ArtistsContentObserver();
            getContentResolver().registerContentObserver(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, true, artistsObserver);

            albumsObserver = new AlbumsContentObserver();
            getContentResolver().registerContentObserver(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, true, albumsObserver);
        }
    }

    private void stopObservingMediaStore() {
        if (isObservingMediaStore) {
            getContentResolver().unregisterContentObserver(artistsObserver);
            getContentResolver().unregisterContentObserver(albumsObserver);
            isObservingMediaStore = false;
        }
    }

    private void startArtistsSync() {
        if (!isTaskActive) {
            isTaskActive = true;

            MediaStoreMigrationTask task = new MediaStoreMigrationTask(this, ArtistsStore.getInstance().getDatabase()) {
                @Override
                protected void onPostExecute(List<ArtistModel> newArtists) {
                    onMediaStoreSyncComplete(newArtists);
                    broadcastMediaStoreSyncComplete();
                }
            };

            task.execute();
        } else {
            isArtistSyncPending = true;
        }
    }

    public void onMediaStoreSyncComplete(List<ArtistModel> newArtists) {
        Log.d("MediaStoreSyncService", "MediaStore sync complete! New artist count: " + newArtists.size());
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(SETTING_HAS_COMPLETED_INITIAL_SYNC, true).apply();
        isTaskActive = false;

        if (!newArtists.isEmpty()) {
            Log.d("MediaStoreSyncService", "Starting GetArtistInfoTask. lastFmEnabled=" + (lastFm != null));
            isTaskActive = true;
            String text = lastFm == null ? getString(R.string.updating_artist_albums) : getString(R.string.fetching_artist_info);
            notificationBuilder.setContentTitle(text).setTicker(text);
            startForeground(NOTIFICATION_ID, notificationBuilder.build());

            GetArtistInfoTask infoTask = new GetArtistInfoTask(this, ArtistsStore.getInstance().getDatabase(), lastFm, newArtists) {
                @Override
                protected void onProgressUpdate(Integer... values) {
                    notificationBuilder.setProgress(values[1], values[0], false);
                    notifyMan.notify(NOTIFICATION_ID, notificationBuilder.build());
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Log.d("MediaStoreSyncService", "GetArtistInfoTask complete!");
                    stopForeground(true);
                    broadcastMediaStoreSyncComplete();
                    isTaskActive = false;

                    startPendingTasks();
                }
            };

            infoTask.execute();
        } else {
            stopForeground(true);
        }
    }

    private void startUpdatingAlbums(final boolean showNotification) {
        if (!isTaskActive) {
            isTaskActive = true;
            Log.d("MediaStoreSyncService", "Starting UpdateCoverArtTask");

            if (showNotification) {
                String text = getString(R.string.updating_artist_albums);
                notificationBuilder.setTicker(text).setContentTitle(text);
                startForeground(NOTIFICATION_ID, notificationBuilder.build());
            }

            UpdateCoverArtTask task = new UpdateCoverArtTask(MediaStoreSyncService.this, ArtistsStore.getInstance().getDatabase()) {
                @Override
                protected void onProgressUpdate(Integer... values) {
                    if (showNotification) {
                        notificationBuilder.setProgress(values[1], values[0], false);
                        notifyMan.notify(NOTIFICATION_ID, notificationBuilder.build());
                    }
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Log.d("MediaStoreSyncService", "Finished updating albums");
                    broadcastMediaStoreSyncComplete();
                    if (showNotification) stopForeground(true);
                    isTaskActive = false;

                    startPendingTasks();
                }
            };

            task.execute();
        }
    }

    private void startLastFmUpdate() {
        if (!isTaskActive && lastFm != null) {
            String text = getString(R.string.fetching_artist_info);
            notificationBuilder.setTicker(text).setContentTitle(text);
            startForeground(NOTIFICATION_ID, notificationBuilder.build());

            LastFmFetchTask task = new LastFmFetchTask(this, ArtistsStore.getInstance().getDatabase(), lastFm) {
                @Override
                protected void onProgressUpdate(Integer... values) {
                    notificationBuilder.setProgress(values[1], values[0], false);
                    notifyMan.notify(NOTIFICATION_ID, notificationBuilder.build());
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Log.d("MediaStoreSyncService", "Finished user requested Last.fm update");
                    broadcastMediaStoreSyncComplete();
                    stopForeground(true);
                    isTaskActive = false;
                    startPendingTasks();
                }
            };

            task.execute();
        }
    }

    @Override
    public void onDestroy() {
        stopObservingMediaStore();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Constants.SETTING_LAST_FM_INTEGRATION.equals(key))
            lastFm = sharedPreferences.getBoolean(key, false) ? createLastFmServer() : null;
    }

    private static LastFmServer createLastFmServer() {
        return LastFmServerFactory.getServer("http://ws.audioscrobbler.com/2.0/",
                "31cf9ad7f222197a94a63d614d28b267",
                "76a5de88fc74f512700849e718567c35");
    }

    private void startPendingTasks() {
        if (isArtistSyncPending) {
            isArtistSyncPending = false;
            startArtistsSync();
        } else if (isAlbumUpdatePending) {
            isAlbumUpdatePending = false;
            startUpdatingAlbums(false);
        }
    }

    private void broadcastMediaStoreSyncComplete() {
        LocalBroadcastManager.getInstance(MediaStoreSyncService.this).sendBroadcast(new Intent(ACTION_MEDIA_STORE_SYNC_COMPLETE));
    }

    private class ArtistsContentObserver extends ContentObserver {
        public ArtistsContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("MediaStoreSyncService", "Change detected in Artists table! isActive()=" + isTaskActive);
            startArtistsSync();
        }
    }

    private class AlbumsContentObserver extends ContentObserver {
        public AlbumsContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("MediaStoreSyncService", "Change detected in Albums table! isActive()=" + isTaskActive);

            if (!isTaskActive) {
                startUpdatingAlbums(false);
            } else {
                isAlbumUpdatePending = true;
            }
        }
    }
}
