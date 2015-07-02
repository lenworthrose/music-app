package com.lenworthrose.music.playback;

import android.database.Cursor;

/**
 * A wrapper around {@link PlaybackService}'s database {@link Cursor}, retrieving relevant
 * fields so that users of this data don't need to know Cursor indices.
 *
 * See {@link PlaylistStore} for the projection used in this query.
 */
public class PlayingItem {
    private String artist, album, title, artUrl;
    private int playlistPosition, trackNum;
    private long duration = -1;

    PlayingItem(Cursor cursor, int position) {
        playlistPosition = position + 1;
        if (cursor == null || cursor.getCount() == 0) return;

        cursor.moveToPosition(position);
        artist = cursor.getString(3);
        album = cursor.getString(4);
        title = cursor.getString(5);
        duration = cursor.getLong(7);
        artUrl = cursor.getString(8);
        trackNum = cursor.getInt(6);
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
}
