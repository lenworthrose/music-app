package com.lenworthrose.music.sync;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import fm.last.api.Artist;
import fm.last.api.LastFmServer;
import fm.last.api.WSError;

/**
 * Helper class to perform Last.fm related tasks.
 */
class LastFmHelper {
    private Context context;
    private LastFmServer lastFm;
    private String artistName, musicBrainzId, bio, artistImageUrl;

    public LastFmHelper(Context context, LastFmServer lastFm, String artistName) {
        this.context = context;
        this.lastFm = lastFm;
        this.artistName = artistName;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public String getBio() {
        return bio;
    }

    public String getArtistImageUrl() {
        return artistImageUrl;
    }

    public void retrieveInfo() {
        try {
            Artist fmArtist = lastFm.getArtistInfo(artistName, null, "en", null);
            musicBrainzId = fmArtist.getMbid();
            bio = fmArtist.getBio().getSummary();

            String megaImgUrl = fmArtist.getURLforImageSize("mega");
            if (megaImgUrl != null) artistImageUrl = retrieveArt(megaImgUrl, musicBrainzId);
        } catch (IOException ex) {
            Log.e("LastFmHelper", "IOException occurred attempting to get info from Last.fm", ex);
        } catch (WSError ex) {
            Log.w("LastFmHelper", "WSError occurred attemping to get info from Last.fm", ex);
        }
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
            Log.e("LastFmHelper", "MalformedURLException occurred attempting to fetch cover art: " + coverArtUrl, ex);
        } catch (IOException ex) {
            Log.e("LastFmHelper", "IOException occurred attempting to fetch cover art", ex);
        }

        return null;
    }
}
