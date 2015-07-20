package com.lenworthrose.music.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link AsyncTask} that is responsible for retrieving the list of Artists from the MediaStore,
 * and inserting these records into our {@link SQLiteDatabase}.
 */
class MediaStoreMigrationTask extends AsyncTask<Cursor, Void, List<ArtistModel>> {
    private SQLiteDatabase db;

    public MediaStoreMigrationTask(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    protected List<ArtistModel> doInBackground(Cursor... params) {
        Cursor artistsCursor = params[0];
        List<ArtistModel> newArtists = new ArrayList<>();

        if (artistsCursor.moveToFirst()) {
            db.beginTransaction();

            try {
                do {
                    //The JavaDoc for this function is incorrect: it will return -1 when using CONFLICT_IGNORE if the row already exists!
                    long id = db.insertWithOnConflict(ArtistsStore.TABLE_NAME, null, createContentValuesFrom(artistsCursor), SQLiteDatabase.CONFLICT_IGNORE);
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

    private static ContentValues createContentValuesFrom(Cursor artistsCursor) {
        ContentValues values = new ContentValues();
        values.put(ArtistsStoreContract.ArtistEntry._ID, artistsCursor.getLong(0));
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_MEDIASTORE_KEY, artistsCursor.getString(3));
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_NAME, artistsCursor.getString(1));
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_NUM_ALBUMS, artistsCursor.getInt(2));
        return values;
    }
}
