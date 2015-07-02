package com.lenworthrose.music.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.lenworthrose.music.R;

import java.util.List;

/**
 * A {@link Service} that listens for changes to the {@link android.provider.MediaStore} database. It will also perform the first
 * sync with the MediaStore database to build the app's own Artist database.
 */
public class MediaStoreService extends Service implements ArtistsStore.InitListener, ArtistsStore.ArtistsStoreListener {
    public static final String ACTION_MEDIA_STORE_SYNC_COMPLETE = "com.lenworthrose.music.sync.MediaStoreService.SYNC_COMPLETE";
    public static final String ACTION_SYNC_WITH_MEDIA_STORE = "com.lenworthrose.music.sync.MediaStoreService.SYNC";
    public static final String ACTION_UPDATE_ALBUMS = "com.lenworthrose.music.sync.MediaStoreService.UPDATE_ALBUMS";

    private static final int NOTIFICATION_ID = 36663;
    private static final String SETTING_HAS_COMPLETED_INITIAL_SYNC = "HasCompletedInitialSync";

    private Cursor artistsCursor, albumsCursor;
    private boolean isObservingMediaStore, isSyncingArtists, isGettingArtistInfo, isUpdatingAlbums, isArtistSyncPending, isAlbumUpdatePending;
    private ContentObserver artistsObserver, albumsObserver;
    private NotificationManager notifyMan;
    private Notification.Builder notificationBuilder;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifyMan = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new Notification.Builder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_SYNC_WITH_MEDIA_STORE:
                    stopObservingMediaStore();
                    startArtistsSync();
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
            artistsCursor = getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[] { "*" }, null, null, null);

            if (!artistsCursor.moveToFirst()) {
                Log.e("MediaStoreService", "Unable to start observing MediaStore's Artists table!");
                artistsCursor.setNotificationUri(getContentResolver(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI);
                isObservingMediaStore = false;
                stopSelf();
            }

            artistsObserver = new ArtistsContentObserver();
            artistsCursor.registerContentObserver(artistsObserver);

            albumsCursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { "*" }, null, null, null);

            if (!albumsCursor.moveToFirst()) {
                Log.e("MediaStoreService", "Unable to start observing MediaStore's Albums table!");
                albumsCursor.close();
            } else {
                albumsCursor.setNotificationUri(getContentResolver(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI);
                albumsObserver = new AlbumsContentObserver();
                albumsCursor.registerContentObserver(albumsObserver);
            }
        }
    }

    private void stopObservingMediaStore() {
        if (isObservingMediaStore) {
            artistsCursor.unregisterContentObserver(artistsObserver);
            artistsCursor.close();

            albumsCursor.unregisterContentObserver(albumsObserver);
            albumsCursor.close();

            isObservingMediaStore = false;
        }
    }

    private void startArtistsSync() {
        if (!isActive()) {
            isSyncingArtists = true;

            String text = getString(R.string.syncing_with_media_store);
            notificationBuilder.setTicker(text).setSmallIcon(R.drawable.sync).setContentTitle(text);
            startForeground(NOTIFICATION_ID, notificationBuilder.build());

            String[] projection = {
                    MediaStore.Audio.Artists._ID,
                    MediaStore.Audio.Artists.ARTIST,
                    MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                    MediaStore.Audio.Artists.ARTIST_KEY
            };

            CursorLoader cursorLoader = new CursorLoader(this, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    projection, null, null, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);

            cursorLoader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
                @Override
                public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
                    ArtistsStore.getInstance().addListener(MediaStoreService.this);
                    ArtistsStore.getInstance().syncFromMediaStore(data);
                }
            });

            cursorLoader.startLoading();
        } else {
            isArtistSyncPending = true;
        }
    }

    @Override
    public void onMediaStoreSyncComplete(List<ArtistModel> newArtists) {
        Log.d("MediaStoreService", "MediaStore sync complete! New artist count: " + newArtists.size());
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(SETTING_HAS_COMPLETED_INITIAL_SYNC, true).commit();
        ArtistsStore.getInstance().removeListener(MediaStoreService.this);
        isSyncingArtists = false;

        if (!isActive()) {
            isGettingArtistInfo = true;
            String text = getString(R.string.fetching_artist_info);
            notificationBuilder.setContentTitle(text).setTicker(text);
            notifyMan.notify(NOTIFICATION_ID, notificationBuilder.build());

            GetArtistInfoTask infoTask = new GetArtistInfoTask(this, newArtists) {
                @Override
                protected void onProgressUpdate(Integer... values) {
                    notificationBuilder.setProgress(values[1], values[0], false);
                    notifyMan.notify(NOTIFICATION_ID, notificationBuilder.build());
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Log.d("MediaStoreService", "GetArtistInfoTask complete!");
                    stopForeground(true);
                    LocalBroadcastManager.getInstance(MediaStoreService.this).sendBroadcast(new Intent(ACTION_MEDIA_STORE_SYNC_COMPLETE));
                    ArtistsStore.getInstance().notifyArtistInfoFetchComplete();
                    isGettingArtistInfo = false;

                    startPendingTasks();
                }
            };

            infoTask.execute();
        }
    }

    @Override
    public void onArtistInfoFetchComplete() { }

    private void startUpdatingAlbums() {
        if (!isActive()) {
            isUpdatingAlbums = true;
            Log.d("MediaStoreService", "startUpdatingAlbums()");
            String text = getString(R.string.updating_artist_albums);
            notificationBuilder.setTicker(text).setContentTitle(text);
            startForeground(NOTIFICATION_ID, notificationBuilder.build());

            UpdateCoverArtTask task = new UpdateCoverArtTask(MediaStoreService.this) {
                @Override
                protected void onProgressUpdate(Integer... values) {
                    notificationBuilder.setProgress(values[1], values[0], false);
                    notifyMan.notify(NOTIFICATION_ID, notificationBuilder.build());
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    LocalBroadcastManager.getInstance(MediaStoreService.this).sendBroadcast(new Intent(ACTION_MEDIA_STORE_SYNC_COMPLETE));
                    ArtistsStore.getInstance().notifyArtistInfoFetchComplete();
                    stopForeground(true);
                    isUpdatingAlbums = false;

                    startPendingTasks();
                }
            };

            task.execute();
        }
    }

    @Override
    public void onDestroy() {
        stopObservingMediaStore();
        super.onDestroy();
    }

    private class ArtistsContentObserver extends ContentObserver {
        public ArtistsContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("MediaStoreService", "Change detected in Artists table! isActive()=" + isActive());
            startArtistsSync();
        }
    }

    private class AlbumsContentObserver extends ContentObserver {
        public AlbumsContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("MediaStoreService", "Change detected in Albums table! isActive()=" + isActive());

            if (!isActive()) {
                startUpdatingAlbums();
            } else {
                isAlbumUpdatePending = true;
            }
        }
    }

    private boolean isActive() {
        return isSyncingArtists || isUpdatingAlbums || isGettingArtistInfo;
    }

    private void startPendingTasks() {
        if (isArtistSyncPending) {
            isArtistSyncPending = false;
            startArtistsSync();
        } else if (isAlbumUpdatePending) {
            isAlbumUpdatePending = false;
            startUpdatingAlbums();
        }
    }
}
