package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.util.List;

import fm.last.api.LastFmServer;

/**
 * Responsible for fetching Artist info and image from Last.fm, as well as querying the Albums DB for cover art.
 */
public class GetArtistInfoTask extends AsyncTask<Void, Integer, Void> {
    private List<ArtistModel> newArtists;
    private Context context;
    private LastFmServer lastFm;

    public GetArtistInfoTask(Context context, LastFmServer lastFm, List<ArtistModel> newArtists) {
        this.context = context;
        this.newArtists = newArtists;
        this.lastFm = lastFm;
    }

    @Override
    protected Void doInBackground(Void... params) {
        int current = 0;

        for (ArtistModel artist : newArtists) {
            publishProgress(current++, newArtists.size());
            String[] albumArt = getAlbumArtUrls(context, artist.getId());
            String mbid = null, bio = null, imgPath = null;

            if (lastFm != null) {
                LastFmHelper helper = new LastFmHelper(context, lastFm, artist.getName());
                helper.retrieveInfo();
                mbid = helper.getMusicBrainzId();
                bio = helper.getBio();
                imgPath = helper.getArtistImageUrl();
            }

            ArtistsStore.getInstance().updateArtist(artist.getId(), mbid, bio, imgPath, albumArt);
        }

        return null;
    }

    private static String[] getAlbumArtUrls(Context context, long artistId) {
        String[] projection = {
                MediaStore.Audio.Albums.ALBUM_ART
        };

        int i = 0;
        String[] albumArt = new String[4];
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection,
                MediaStore.Audio.Media.ARTIST_ID + "=?", new String[] { String.valueOf(artistId) }, MediaStore.Audio.Albums.ALBUM_KEY + " DESC");

        if (cursor.moveToFirst())
            do {
                if (cursor.getString(0) != null)
                    albumArt[i++] = cursor.getString(0);
            } while (cursor.moveToNext() && i < 4);

        cursor.close();

        return albumArt;
    }
}
