package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.lenworthrose.music.util.Utils;

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
                String[] albumArtUrls = Utils.getAlbumArtUrls(context, artistId);

                if (albumArtUrls != null)
                    ArtistsStore.getInstance().updateArtistAlbumArt(artistId, albumArtUrls);
                else
                    ArtistsStore.getInstance().removeArtist(artistId);
            } while (artistsCursor.moveToNext());
        }

        artistsCursor.close();

        return null;
    }
}
