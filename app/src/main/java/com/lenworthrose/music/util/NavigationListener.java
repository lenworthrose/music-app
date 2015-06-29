package com.lenworthrose.music.util;

import android.database.Cursor;

import java.util.ArrayList;

public interface NavigationListener {
    void onNavigateToArtist(long artistId);
    void onNavigateToAlbum(long albumId);
    void playSongs(Cursor songsCursor, int from);
    void playAlbums(ArrayList<Long> albumId);
    void playArtists(ArrayList<Long> artistId);
    void addAlbums(ArrayList<Long> albumId);
    void addArtists(ArrayList<Long> artistId);
    void addAlbumsAsNext(ArrayList<Long> albumId);
    void addArtistsAsNext(ArrayList<Long> artistId);
}
