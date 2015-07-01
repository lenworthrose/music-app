package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.lenworthrose.music.sql.SqlArtistsStore;

import java.util.List;

/**
 * Responsible for fetching info from MusicBrainz, as well as querying the Albums DB for cover art.
 */
public class GetArtistInfoTask extends AsyncTask<Void, Void, Void> {
    private List<SqlArtistsStore.ArtistModel> newArtists;
    private Context context;

    public GetArtistInfoTask(Context context, List<SqlArtistsStore.ArtistModel> newArtists) {
        this.context = context;
        this.newArtists = newArtists;
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (SqlArtistsStore.ArtistModel artist : newArtists) {
            String[] project = {
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ALBUM_ART
            };

            int i = 0;
            String[] albumArt = new String[4];
            Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, project,
                    MediaStore.Audio.Media.ARTIST_ID + "=?", new String[] { String.valueOf(artist.getId()) }, null);

            if (cursor.moveToFirst())
                do {
                    albumArt[i++] = cursor.getString(1);
                } while (cursor.moveToNext() && i < 4);

            cursor.close();

            SqlArtistsStore.getInstance().updateArtist(artist.getId(), null, null, albumArt);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {

    }
}
