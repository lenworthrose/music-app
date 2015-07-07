package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;

/**
 * AsyncTask that updates the cover art for all artists in the DB. Will also remove any artists
 * entries if there are no albums for the artist.
 */
public class UpdateCoverArtTask extends AsyncTask<Void, Integer, Void> {
    private Context context;

    public UpdateCoverArtTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Cursor artistsCursor = ArtistsStore.getInstance().getArtists();

        if (artistsCursor.moveToFirst()) {
            int current = 0, total = artistsCursor.getCount();

            do {
                publishProgress(current++, total);
                long artistId = artistsCursor.getLong(0);

                String[] projection = {
                        MediaStore.Audio.Albums.ALBUM_ART
                };

                int i = 0;

                Cursor albumsCursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection,
                        MediaStore.Audio.Media.ARTIST_ID + "=?", new String[] { String.valueOf(artistId) }, MediaStore.Audio.Albums.ALBUM_KEY + " DESC");

                if (albumsCursor.moveToFirst()) {
                    String[] albumArt = new String[4];

                    do {
                        if (albumsCursor.getString(0) != null)
                            albumArt[i++] = albumsCursor.getString(0);
                    } while (albumsCursor.moveToNext() && i < 4);

                    ArtistsStore.getInstance().updateArtistAlbumArt(artistId, albumArt);
                } else {
                    ArtistsStore.getInstance().removeArtist(artistsCursor.getString(1));
                }

                albumsCursor.close();
            } while (artistsCursor.moveToNext());
        }

        artistsCursor.close();

        return null;
    }
}
