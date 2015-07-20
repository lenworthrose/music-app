package com.lenworthrose.music.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.lenworthrose.music.util.Utils;

/**
 * {@link AsyncTask} that updates the cover art for all artists in the DB. Will also remove any artists
 * entries if there are no albums for the artist.
 */
public class UpdateCoverArtTask extends AsyncTask<Void, Integer, Void> {
    private Context context;
    private SQLiteDatabase db;

    public UpdateCoverArtTask(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Cursor artistsCursor = ArtistsStore.getInstance().getArtists();

        if (artistsCursor.moveToFirst()) {
            int current = 0, total = artistsCursor.getCount();
            db.beginTransactionNonExclusive();

             try {
                 do {
                     publishProgress(current++, total);
                     long artistId = artistsCursor.getLong(0);
                     String[] albumArtUrls = Utils.getAlbumArtUrls(context, artistId);

                     if (albumArtUrls != null)
                         updateArtistAlbumArt(artistId, albumArtUrls);
                     else
                         removeArtist(artistId);
                 } while (artistsCursor.moveToNext());

                 db.setTransactionSuccessful();
             } finally {
                 db.endTransaction();
             }
        }

        artistsCursor.close();

        return null;
    }

    private void updateArtistAlbumArt(long id, String... albumArtUris) {
        ContentValues values = new ContentValues();

        if (albumArtUris != null) {
            values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_1, albumArtUris[0]);
            values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_2, albumArtUris[1]);
            values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_3, albumArtUris[2]);
            values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_4, albumArtUris[3]);
        }

        db.update(ArtistsStore.TABLE_NAME, values, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) });
    }

    private void removeArtist(long artistId) {
        db.delete(ArtistsStore.TABLE_NAME, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(artistId) });
    }
}
