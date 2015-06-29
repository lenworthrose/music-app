package com.lenworthrose.music.playback;

import android.database.Cursor;

/**
 * See {@link SqlPlaylistStore} for the projection used in this query.
 */
public class PlayingItem {
    private Cursor cursor;
    private int playlistPosition;

    PlayingItem(Cursor cursor, int position) {
        this.cursor = cursor;
        playlistPosition = position;
        if (cursor != null) cursor.moveToPosition(position);
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

    public long getAlbumId() {
        return cursor == null ? -1 : cursor.getLong(8);
    }
}
