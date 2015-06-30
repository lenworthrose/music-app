package com.lenworthrose.music.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Artists database.
 */
public class SqlArtistsStore {
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
            ArtistsStoreContract.ArtistEntry.COLUMN_MUSICBRAINZ_WIKI_INFO
    };

    private SQLiteDatabase db;

    public Cursor getArtists() {
        return db.query(TABLE_NAME, PROJECTION_ALL, null, null, null, null, ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY);
    }

    public Cursor getArtistInfo(long id) {
        return db.query(TABLE_NAME, PROJECTION_ONE, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
    }

    public void updateArtist(long id, String musicBrainzId, String musicBrainzInfo, String... albumArtUris) {
        ContentValues values = new ContentValues();
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_MUSICBRAINZ_ID, musicBrainzId);
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_MUSICBRAINZ_WIKI_INFO, musicBrainzInfo);

        if (albumArtUris != null) {
            try {
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_1, albumArtUris[0]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_2, albumArtUris[1]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_3, albumArtUris[2]);
                values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_4, albumArtUris[3]);
            } catch (IndexOutOfBoundsException ex) {
                Log.d("SqlArtistsStore", "IndexOutOfBoundsException occurred attempting to add album art to ContentValues: albumArtUris.length=" + albumArtUris.length);
            }
        }

        db.update(TABLE_NAME, values, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) });
    }

    public void populateFromMediaStore(Cursor artistsCursor) {
        MediaStoreMigrationTask task = new MediaStoreMigrationTask();
        task.execute(artistsCursor);
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
                        long id = db.insertWithOnConflict(TABLE_NAME, null, createContentValuesFrom(artistsCursor), SQLiteDatabase.CONFLICT_ABORT);
                        if (id != -1) newArtists.add(new ArtistModel(id, artistsCursor.getString(1)));
                    } while (artistsCursor.moveToNext());

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            return newArtists;
        }

        @Override
        protected void onPostExecute(List<ArtistModel> newArtists) {

        }
    }

    private static ContentValues createContentValuesFrom(Cursor artistsCursor) {
        ContentValues values = new ContentValues();
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY, artistsCursor.getString(3));
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_NAME, artistsCursor.getString(1));
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_NUM_ALBUMS, artistsCursor.getInt(2));
        return values;
    }

    private class ArtistModel {
        private String name;
        private long id;

        public ArtistModel(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    // -------- Boring initialization code

    public interface InitListener {
        void onArtistsDbInitialized();
    }

    private InitTask initTask;
    private InitListener initListener;

    public void init(Context context, InitListener listener) {
        initListener = listener;
        initTask = new InitTask();
        initTask.execute(context);
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
            initTask = null;

            if (initListener != null) {
                initListener.onArtistsDbInitialized();
                initListener = null;
            }
        }
    }
}
