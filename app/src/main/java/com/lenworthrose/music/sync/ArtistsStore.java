package com.lenworthrose.music.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Artists database.
 */
public class ArtistsStore {
    public interface InitListener {
        void onArtistsDbInitialized();
    }

    public interface ArtistsStoreListener {
        void onMediaStoreSyncComplete(List<ArtistModel> newArtists);
        void onArtistInfoFetchComplete();
    }

    private static final class LazyHolder {
        static ArtistsStore INSTANCE = new ArtistsStore();
    }

    public static ArtistsStore getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final String TABLE_NAME = "artists";

    private static final String[] PROJECTION_ALL = {
            ArtistsStoreContract.ArtistEntry._ID,
            ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY,
            ArtistsStoreContract.ArtistEntry.COLUMN_NAME,
            ArtistsStoreContract.ArtistEntry.COLUMN_NUM_ALBUMS,
            ArtistsStoreContract.ArtistEntry.COLUMN_ARTIST_ART_FILE_URL,
            ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_1,
            ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_2,
            ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_3,
            ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_4
    };

    private static final String[] PROJECTION_ONE = {
            ArtistsStoreContract.ArtistEntry._ID,
            ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY,
            ArtistsStoreContract.ArtistEntry.COLUMN_NAME,
            ArtistsStoreContract.ArtistEntry.COLUMN_NUM_ALBUMS,
            ArtistsStoreContract.ArtistEntry.COLUMN_ARTIST_ART_FILE_URL,
            ArtistsStoreContract.ArtistEntry.COLUMN_MUSICBRAINZ_ID,
            ArtistsStoreContract.ArtistEntry.COLUMN_BIO
    };

    private SQLiteDatabase db;
    private InitTask initTask;
    private boolean isInitializing, isInitialized;
    private List<InitListener> initListeners = new ArrayList<>(3);
    private List<WeakReference<ArtistsStoreListener>> artistsStoreListeners = new ArrayList<>(3);

    private ArtistsStore() { }

    public void init(Context context, InitListener listener) {
        if (isInitialized) {
            listener.onArtistsDbInitialized();
        } else {
            initListeners.add(listener);

            if (!isInitializing) {
                isInitializing = true;
                initTask = new InitTask();
                initTask.execute(context);
            }
        }
    }

    public Cursor getArtists() {
        return getArtists(null);
    }

    public Cursor getArtists(String filter) {
        String where = null;
        String[] whereArgs = null;

        if (filter != null) {
            where = ArtistsStoreContract.ArtistEntry.COLUMN_NAME + " LIKE ?";
            whereArgs = new String[] { '%' + filter + '%' };
        }

        return db.query(TABLE_NAME,
                PROJECTION_ALL,
                where,
                whereArgs,
                null,
                null,
                ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY);
    }

    public CursorLoader getArtistInfo(Context context, final long id) {
        return new CursorLoader(context) {
            @Override
            public Cursor loadInBackground() {
                return db.query(TABLE_NAME, PROJECTION_ONE, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
            }
        };
    }

    void updateArtist(long id, String musicBrainzId, String lastFmInfo, String artistImgUrl, String... albumArtUris) {
        ContentValues values = new ContentValues();
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_MUSICBRAINZ_ID, musicBrainzId);
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_BIO, lastFmInfo);
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ARTIST_ART_FILE_URL, artistImgUrl);

        if (albumArtUris != null) {
            try {
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_1, albumArtUris[0]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_2, albumArtUris[1]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_3, albumArtUris[2]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_4, albumArtUris[3]);
            } catch (IndexOutOfBoundsException ex) {
                Log.d("ArtistsStore", "IndexOutOfBoundsException occurred attempting to add album art to ContentValues: albumArtUris.length=" + albumArtUris.length);
            }
        }

        db.update(TABLE_NAME, values, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) });
    }

    void updateArtist(long id, String... albumArtUris) {
        ContentValues values = new ContentValues();

        if (albumArtUris != null) {
            try {
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_1, albumArtUris[0]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_2, albumArtUris[1]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_3, albumArtUris[2]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_4, albumArtUris[3]);
            } catch (IndexOutOfBoundsException ex) {
                Log.d("ArtistsStore", "IndexOutOfBoundsException occurred attempting to add album art to ContentValues: albumArtUris.length=" + albumArtUris.length);
            }
        }

        db.update(TABLE_NAME, values, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) });
    }

    void removeArtist(String artistKey) {
        db.delete(TABLE_NAME, ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY + "=?", new String[] { artistKey });
    }

    void syncFromMediaStore(Cursor artistsCursor) {
        MediaStoreMigrationTask task = new MediaStoreMigrationTask();
        task.execute(artistsCursor);
    }

    void notifyArtistInfoFetchComplete() {
        for (int i = artistsStoreListeners.size() - 1; i >= 0; i--) {
            WeakReference<ArtistsStoreListener> listener = artistsStoreListeners.get(i);
            if (listener.get() != null) listener.get().onArtistInfoFetchComplete();
        }
    }

    private class MediaStoreMigrationTask extends AsyncTask<Cursor, Void, List<ArtistModel>> {
        @Override
        protected List<ArtistModel> doInBackground(Cursor... params) {
            Cursor artistsCursor = params[0];
            List<ArtistModel> newArtists = new ArrayList<>();

            if (artistsCursor.moveToFirst()) {
                db.beginTransaction();

                try {
                    do {
                        long id = db.insertWithOnConflict(TABLE_NAME, null, createContentValuesFrom(artistsCursor), SQLiteDatabase.CONFLICT_IGNORE);
                        if (id != -1) newArtists.add(new ArtistModel(id, artistsCursor.getString(1)));
                    } while (artistsCursor.moveToNext());

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            artistsCursor.close();
            return newArtists;
        }

        @Override
        protected void onPostExecute(List<ArtistModel> newArtists) {
            for (int i = artistsStoreListeners.size() - 1; i >= 0; i--) {
                WeakReference<ArtistsStoreListener> listener;

                try {
                    listener = artistsStoreListeners.get(i);
                } catch (IndexOutOfBoundsException ex) {
                    continue;
                }

                if (listener.get() != null) listener.get().onMediaStoreSyncComplete(newArtists);
            }
        }
    }

    public void addListener(ArtistsStoreListener listener) {
        artistsStoreListeners.add(new WeakReference<>(listener));
    }

    public void removeListener(ArtistsStoreListener artistsStoreListener) {
        for (int i = artistsStoreListeners.size() - 1; i >= 0; i--) {
            WeakReference<ArtistsStoreListener> listener = artistsStoreListeners.get(i);

            if (listener.get() == null || listener.get() == artistsStoreListener)
                artistsStoreListeners.remove(i);
        }
    }

    private class InitTask extends AsyncTask<Context, Void, Void> {
        @Override
        protected Void doInBackground(Context... params) {
            ArtistsStoreDbHelper helper = new ArtistsStoreDbHelper(params[0]);
            db = helper.getReadableDatabase();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            isInitialized = true;
            initTask = null;

            for (int i = initListeners.size() - 1; i >= 0; i--)
                initListeners.get(i).onArtistsDbInitialized();

            initListeners.clear();
        }
    }

    private static ContentValues createContentValuesFrom(Cursor artistsCursor) {
        ContentValues values = new ContentValues();
        values.put(ArtistsStoreContract.ArtistEntry._ID, artistsCursor.getLong(0));
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY, artistsCursor.getString(3));
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_NAME, artistsCursor.getString(1));
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_NUM_ALBUMS, artistsCursor.getInt(2));
        return values;
    }
}
