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

    private static final int NOTIFICATION_ID = 36663;
    private static final String SETTING_HAS_COMPLETED_INITIAL_SYNC = "HasCompletedInitialSync";

    private Cursor cursor;
    private boolean isObservingMediaStore, isSyncingWithMediaStore;
    private ContentObserver observer;
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

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SETTING_HAS_COMPLETED_INITIAL_SYNC, false))
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

            String text = getString(R.string.syncing_with_media_store);
            notificationBuilder = new Notification.Builder(this);
            notificationBuilder.setTicker(text).setSmallIcon(R.drawable.sync).setContentTitle(text);
            startForeground(NOTIFICATION_ID, notificationBuilder.build());

            ArtistsStore.getInstance().init(MediaStoreService.this, MediaStoreService.this);
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
                ArtistsStore.getInstance().addListener(MediaStoreService.this);
                ArtistsStore.getInstance().syncFromMediaStore(data);
            }
        });
        cursorLoader.startLoading();
    }

    @Override
    public void onMediaStoreSyncComplete(List<ArtistModel> newArtists) {
        Log.d("MediaStoreService", "MediaStore sync complete! New artist count: " + newArtists.size());
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(SETTING_HAS_COMPLETED_INITIAL_SYNC, true).commit();
        ArtistsStore.getInstance().removeListener(MediaStoreService.this);
        isSyncingWithMediaStore = false;

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
                notificationBuilder = null;
                LocalBroadcastManager.getInstance(MediaStoreService.this).sendBroadcast(new Intent(ACTION_MEDIA_STORE_SYNC_COMPLETE));
            }
        };

        infoTask.execute();
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
