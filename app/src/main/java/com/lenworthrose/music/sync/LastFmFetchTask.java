package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import fm.last.api.LastFmServer;

/**
 * AsyncTask that retrieves Artist information from Last.fm.
 */
public class LastFmFetchTask extends AsyncTask<Void, Integer, Void> {
    private Context context;
    private LastFmServer lastFm;

    public LastFmFetchTask(Context context, LastFmServer lastFm) {
        this.context = context;
        this.lastFm = lastFm;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Cursor artistsCursor = ArtistsStore.getInstance().getArtistsWithoutInfo();

        if (artistsCursor.moveToFirst()) {
            int count = artistsCursor.getCount(), current = 0;

            do {
                publishProgress(current++, count);

                LastFmHelper helper = new LastFmHelper(context, lastFm, artistsCursor.getString(2));
                helper.retrieveInfo();

                ArtistsStore.getInstance().updateArtistLastFmInfo(artistsCursor.getLong(0),
                        helper.getMusicBrainzId(),
                        helper.getBio(),
                        helper.getArtistImageUrl());
            } while (artistsCursor.moveToNext());
        }

        artistsCursor.close();

        return null;
    }
}
