package com.lenworthrose.music.playback;

import android.database.Cursor;

/**
 * A wrapper around {@link PlaybackService}'s database {@link Cursor}, providing a way
 * to get information about the item without needing to know the cursor's column indices.
 *
 * See {@link PlaylistStore} for the projection used in this query.
 */
public class PlayingItem {
    private Cursor cursor;
    private int playlistPosition;

    PlayingItem(Cursor cursor, int position) {
        if (cursor == null || cursor.getCount() == 0) return;

        this.cursor = cursor;
        playlistPosition = position;
        cursor.moveToPosition(position);
    }

    public String getArtist() {
        return cursor == null ? null : cursor.getString(3);
    }

    public String getAlbum() {
        return cursor == null ? null : cursor.getString(4);
    }

    public String getTitle() {
        return cursor == null ? null : cursor.getString(5);
    }

    public long getDuration() {
        return cursor == null ? -1 : cursor.getLong(7);
    }

    public int getPlaylistPosition() {
        return playlistPosition + 1;
    }

    public String getAlbumArtUrl() {
        return cursor == null ? null : cursor.getString(8);
    }

    public int getTrackNum() {
        return cursor == null ? -1 : cursor.getInt(6);
    }
}
