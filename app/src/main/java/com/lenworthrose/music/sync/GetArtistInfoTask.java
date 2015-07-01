package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import fm.last.api.Artist;
import fm.last.api.LastFmServer;
import fm.last.api.LastFmServerFactory;

/**
 * Responsible for fetching Artist info and image from Last.fm, as well as querying the Albums DB for cover art.
 */
public class GetArtistInfoTask extends AsyncTask<Void, Integer, Void> {
    private List<ArtistModel> newArtists;
    private Context context;
    private LastFmServer lastFm;

    public GetArtistInfoTask(Context context, List<ArtistModel> newArtists) {
        this.context = context;
        this.newArtists = newArtists;
        lastFm = LastFmServerFactory.getServer("http://ws.audioscrobbler.com/2.0/", "31cf9ad7f222197a94a63d614d28b267", "76a5de88fc74f512700849e718567c35");
    }

    @Override
    protected Void doInBackground(Void... params) {
        int current = 0;

        for (ArtistModel artist : newArtists) {
            publishProgress(current++, newArtists.size());
            String[] albumArt = getAlbumArtUrls(artist);

            String mbid = null, bio = null, imgPath = null;

            try {
                Artist fmArtist = lastFm.getArtistInfo(artist.getName(), null, "en", null);
                mbid = fmArtist.getMbid();
                bio = fmArtist.getBio().getSummary();

                String megaImgUrl = fmArtist.getURLforImageSize("mega");
                if (megaImgUrl != null) imgPath = retrieveArt(megaImgUrl, mbid);
            } catch (IOException ex) {
                Log.e("GetArtistInfoTask", "IOException occurred attempting to get info from Last.fm", ex);
            }

            ArtistsStore.getInstance().updateArtist(artist.getId(), mbid, bio, imgPath, albumArt);
        }

        return null;
    }

    private String[] getAlbumArtUrls(ArtistModel artist) {
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

        cursor.close();

        return albumArt;
    }

    private String retrieveArt(String coverArtUrl, String fileName) {
        try {
            URL url = new URL(coverArtUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.connect();

            if (conn.getResponseCode() == 200) {
                File file = new File(context.getFilesDir(), fileName + ".jpg");
                InputStream inputStream = conn.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1)
                    outputStream.write(buffer, 0, bytesRead);

                inputStream.close();
                outputStream.close();

                return file.getAbsolutePath();
            }
        } catch (MalformedURLException ex) {
            Log.e("GetArtistInfoTask", "MalformedURLException occurred attempting to fetch cover art: " + coverArtUrl, ex);
        } catch (IOException ex) {
            Log.e("GetArtistInfoTask", "IOException occurred attempting to fetch cover art", ex);
        }

        return null;
    }
}
