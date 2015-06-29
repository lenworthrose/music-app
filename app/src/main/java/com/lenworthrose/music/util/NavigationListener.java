package com.lenworthrose.music.util;

/**
 * Created by Lenny on 2015-06-25.
 */
public interface NavigationListener {
    void onNavigateToArtist(long artistId);
    void onNavigateToAlbum(long albumId);
}
