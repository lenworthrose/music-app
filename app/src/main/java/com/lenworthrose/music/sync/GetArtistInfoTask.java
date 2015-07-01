package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.lenworthrose.music.sql.SqlArtistsStore;

import java.io.IOException;
import java.util.List;

import fm.last.api.Artist;
import fm.last.api.LastFmServer;
import fm.last.api.LastFmServerFactory;

/**
 * Responsible for fetching info from MusicBrainz, as well as querying the Albums DB for cover art.
 */
public class GetArtistInfoTask extends AsyncTask<Void, Void, Void> {
    private List<SqlArtistsStore.ArtistModel> newArtists;
    private Context context;
    private LastFmServer lastFm;

    public GetArtistInfoTask(Context context, List<SqlArtistsStore.ArtistModel> newArtists) {
        this.context = context;
        this.newArtists = newArtists;
        lastFm = LastFmServerFactory.getServer("http://ws.audioscrobbler.com/2.0/", "31cf9ad7f222197a94a63d614d28b267", "76a5de88fc74f512700849e718567c35");
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (SqlArtistsStore.ArtistModel artist : newArtists) {
            String[] projection = {
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.ALBUM_ART
            };

            int i = 0;
            String[] albumArt = new String[4];
            Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection,
                    MediaStore.Audio.Media.ARTIST_ID + "=?", new String[] { String.valueOf(artist.getId()) }, null);

            if (cursor.moveToFirst())
                do {
                    albumArt[i++] = cursor.getString(2);
                } while (cursor.moveToNext() && i < 4);

            String mbid = null, bio = null;

            try {
                Artist fmArtist = lastFm.getArtistInfo(artist.getName(), null, "en", null);
                mbid = fmArtist.getMbid();
                bio = fmArtist.getBio().getSummary();
            } catch (IOException ex) {
                Log.e("GetArtistInfoTask", "IOException occurred attempting to get info from Last.fm", ex);
            }

            cursor.close();

            SqlArtistsStore.getInstance().updateArtist(artist.getId(), mbid, bio, albumArt);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {

    }
}
