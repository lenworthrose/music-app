package com.lenworthrose.music;

import android.app.Notification;
import android.app.Service;
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

import com.lenworthrose.music.sql.SqlArtistsStore;

import java.util.List;

/**
 * A {@link Service} that listens for changes to the {@link android.provider.MediaStore} database. It will also perform the first
 * sync with the MediaStore database to build the app's own Artist database.
 */
public class MediaStoreService extends Service implements SqlArtistsStore.InitListener, SqlArtistsStore.ArtistsStoreListener {
    public static final String ACTION_MEDIA_STORE_SYNC_COMPLETE = "com.lenworthrose.music.MediaStoreService.SYNC_COMPLETE";
    public static final String ACTION_SYNC_WITH_MEDIA_STORE = "com.lenworthrose.music.MediaStoreService.SYNC";

    private static final int NOTIFICATION_ID = 36663;
    private static final String PREF_HAS_SYNCED = "HasSyncedWithMediaStore";

    private Cursor cursor;
    private boolean isObservingMediaStore, isSyncingWithMediaStore;
    private ContentObserver observer;
    private LocalBroadcastManager broadcastMan;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        broadcastMan = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_SYNC_WITH_MEDIA_STORE:
                    stopObservingMediaStore();
                    startMediaStoreSync();
                    return START_STICKY;
            }
        }

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREF_HAS_SYNCED, false))
            startMediaStoreSync();
        else
            startObservingMediaStore();

        return START_STICKY;
    }

    private void startObservingMediaStore() {
        if (!isObservingMediaStore) {
            isObservingMediaStore = true;
            cursor = getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[] { "*" }, null, null, null);

            if (!cursor.moveToFirst()) {
                Log.e("MediaStoreService", "Unable to start observing MediaStore's Artists table!");
                isObservingMediaStore = false;
                stopSelf();
            }

            observer = new ArtistsContentObserver();
            cursor.registerContentObserver(observer);
        }
    }

    private void stopObservingMediaStore() {
        if (isObservingMediaStore) {
            cursor.unregisterContentObserver(observer);
            cursor.close();
            isObservingMediaStore = false;
        }
    }

    private void startMediaStoreSync() {
        if (!isSyncingWithMediaStore) {
            isSyncingWithMediaStore = true;

            Notification.Builder b = new Notification.Builder(MediaStoreService.this);
            b.setTicker(getString(R.string.syncing_with_media_store)).setSmallIcon(R.drawable.sync);
            b.setContentTitle(getString(R.string.syncing_with_media_store));
            startForeground(NOTIFICATION_ID, b.build());

            SqlArtistsStore.getInstance().addListener(this);
            SqlArtistsStore.getInstance().init(MediaStoreService.this, MediaStoreService.this);
        }
    }

    @Override
    public void onArtistsDbInitialized() {
        String[] projection = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.ARTIST_KEY
        };

        CursorLoader cursorLoader = new CursorLoader(this, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
        cursorLoader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
            @Override
            public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
                SqlArtistsStore.getInstance().syncFromMediaStore(data);
            }
        });
        cursorLoader.startLoading();
    }

    @Override
    public void onMediaStoreSyncComplete(List<SqlArtistsStore.ArtistModel> newArtists) {
        //TODO: create a new AsyncTask that will perform the following:
        //TODO: Retrieve content from MusicBrainz
        //TODO: Retrieve list of cover art from MediaStore

        isSyncingWithMediaStore = false;
        SqlArtistsStore.getInstance().removeListener(this);
        Log.d("MediaStoreService", "MediaStore sync complete! New artist count: " + newArtists.size());
        stopForeground(true);
        broadcastMan.sendBroadcast(new Intent(ACTION_MEDIA_STORE_SYNC_COMPLETE));
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

        }
    }
}
