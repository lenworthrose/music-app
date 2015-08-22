package com.lenworthrose.music.playback;

import android.database.Cursor;
import android.util.Log;

/**
 * A wrapper around {@link PlaybackService}'s database {@link Cursor}, retrieving relevant
 * fields so that users of this data don't need to know Cursor indices.
 *
 * See {@link PlaylistStore} for the projection used in this query.
 */
public class PlayingItem {
    private String artist, album, title, artUrl;
    private int playlistPosition, trackNum;
    private long duration = -1, artistId = -1, albumId = -1;

    PlayingItem(Cursor cursor, int position) {
        playlistPosition = position + 1;
        if (cursor == null || cursor.getCount() == 0) return;

        try {
            cursor.moveToPosition(position);
            artist = cursor.getString(3);
            album = cursor.getString(4);
            title = cursor.getString(5);
            duration = cursor.getLong(7);
            artUrl = cursor.getString(8);
            trackNum = cursor.getInt(6);
            artistId = cursor.getLong(10);
            albumId = cursor.getLong(9);
        } catch (IllegalArgumentException ex) {
            Log.e("PlayingItem", "IllegalArgumentException occurred attempting to populate PlayingItem", ex);
        }
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public long getDuration() {
        return duration;
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public String getAlbumArtUrl() {
        return artUrl;
    }

    public int getTrackNum() {
        return trackNum;
    }

    public long getArtistId() {
        return artistId;
    }

    public long getAlbumId() {
        return albumId;
    }
}
