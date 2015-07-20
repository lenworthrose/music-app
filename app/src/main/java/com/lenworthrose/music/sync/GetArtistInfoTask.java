package com.lenworthrose.music.sync;

import android.content.Context;
import android.os.AsyncTask;

import com.lenworthrose.music.util.Utils;

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
            String mbid = null, bio = null, artistImgPath = null;

            if (lastFm != null) {
                LastFmHelper helper = new LastFmHelper(context, lastFm, artist.getName());
                helper.retrieveInfo();
                mbid = helper.getMusicBrainzId();
                bio = helper.getBio();
                artistImgPath = helper.getArtistImageUrl();
            }

            String[] albumArt = Utils.getAlbumArtUrls(context, artist.getId());
            ArtistsStore.getInstance().updateArtist(artist.getId(), mbid, bio, artistImgPath, albumArt);
        }

        return null;
    }
}
