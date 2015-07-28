package com.lenworthrose.music.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.lenworthrose.music.util.Utils;

import java.util.List;

import fm.last.api.LastFmServer;

/**
 * Responsible for fetching Artist info and image from Last.fm, as well as querying the Albums DB for cover art.
 */
class GetArtistInfoTask extends AsyncTask<Void, Integer, Void> {
    private Context context;
    private SQLiteDatabase db;
    private LastFmServer lastFm;
    private List<ArtistModel> newArtists;

    public GetArtistInfoTask(Context context, SQLiteDatabase db, LastFmServer lastFm, List<ArtistModel> newArtists) {
        this.context = context;
        this.db = db;
        this.newArtists = newArtists;
        this.lastFm = lastFm;
    }

    @Override
    protected Void doInBackground(Void... params) {
        int current = 0;
        db.beginTransactionNonExclusive();

        try {
            for (ArtistModel artist : newArtists) {
                publishProgress(current++, newArtists.size());
                String mbid = null, bio = null, artistImgPath = null;

                if (lastFm != null) {
                    LastFmHelper helper = new LastFmHelper(context, lastFm, artist.getName());
                    helper.retrieveInfo();
                    mbid = helper.getMusicBrainzId();
                    bio = helper.getBio();
                    artistImgPath = helper.getArtistImageUrl();
                }

                String[] albumArt = Utils.getAlbumArtUrls(context, artist.getId());
                updateArtist(artist.getId(), mbid, bio, artistImgPath, albumArt);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return null;
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

        db.update(ArtistsStore.TABLE_NAME, values, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) });
    }
}
