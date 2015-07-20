package com.lenworthrose.music.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import fm.last.api.LastFmServer;

/**
 * {@link AsyncTask} that retrieves Artist information from Last.fm.
 */
class LastFmFetchTask extends AsyncTask<Void, Integer, Void> {
    private Context context;
    private SQLiteDatabase db;
    private LastFmServer lastFm;

    public LastFmFetchTask(Context context, SQLiteDatabase db, LastFmServer lastFm) {
        this.context = context;
        this.db = db;
        this.lastFm = lastFm;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Cursor artistsCursor = ArtistsStore.getInstance().getArtistsWithoutLastFmInfo();

        if (artistsCursor.moveToFirst()) {
            db.beginTransaction();

            try {
                int count = artistsCursor.getCount(), current = 0;

                do {
                    publishProgress(current++, count);

                    LastFmHelper helper = new LastFmHelper(context, lastFm, artistsCursor.getString(2));
                    helper.retrieveInfo();

                    updateArtistLastFmInfo(artistsCursor.getLong(0),
                            helper.getMusicBrainzId(),
                            helper.getBio(),
                            helper.getArtistImageUrl());
                } while (artistsCursor.moveToNext());

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        artistsCursor.close();

        return null;
    }

    void updateArtistLastFmInfo(long id, String musicBrainzId, String bio, String artistImgUrl) {
        ContentValues values = new ContentValues();
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_MUSICBRAINZ_ID, musicBrainzId);
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_BIO, bio);
        values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ARTIST_ART_FILE_URL, artistImgUrl);

        db.update(ArtistsStore.TABLE_NAME, values, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) });
    }
}
