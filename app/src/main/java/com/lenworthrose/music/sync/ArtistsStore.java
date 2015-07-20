package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.content.CursorLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Artists database.
 * <p/>
 * Note: the primary key "_ID" for our artists table is the same as the primary key "_ID" in MediaStore's
 * Artists table!
 */
public class ArtistsStore {
    public interface InitListener {
        void onArtistsDbInitialized();
    }

    private static final class LazyHolder {
        static ArtistsStore INSTANCE = new ArtistsStore();
    }

    public static ArtistsStore getInstance() {
        return LazyHolder.INSTANCE;
    }

    static final String TABLE_NAME = "artists";

    private static final String[] PROJECTION_ALL = {
            ArtistsStoreContract.ArtistEntry._ID,
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

    public Cursor getArtistsWithoutLastFmInfo() {
        return db.query(TABLE_NAME,
                PROJECTION_ALL,
                ArtistsStoreContract.ArtistEntry.COLUMN_BIO + " IS NULL",
                null,
                null,
                null,
                ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY);
    }

    public CursorLoader getArtistInfo(Context context, final long artistId) {
        return new CursorLoader(context) {
            @Override
            public Cursor loadInBackground() {
                return db.query(TABLE_NAME,
                        PROJECTION_ONE,
                        ArtistsStoreContract.ArtistEntry._ID + "=?",
                        new String[] { String.valueOf(artistId) },
                        null,
                        null,
                        null);
            }
        };
    }

    SQLiteDatabase getDatabase() {
        return db;
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
}
