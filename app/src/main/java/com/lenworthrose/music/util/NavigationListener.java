package com.lenworthrose.music.util;

import android.database.Cursor;

public interface NavigationListener {
    void onNavigateToArtist(long artistId);
    void onNavigateToAlbum(long albumId);
    void playSongs(Cursor songsCursor, int from);
}
