package com.lenworthrose.music.loader;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

public class AlbumLoaderCallbacks extends LoaderCallbacks {
    private long artistId;

    public AlbumLoaderCallbacks(long artistId, Context context, CursorAdapter adapter) {
        super(context, adapter);
        this.artistId = artistId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                MediaStore.Audio.Albums.FIRST_YEAR,
                MediaStore.Audio.Albums.ALBUM_ART
        };

        String[] whereVars = { String.valueOf(artistId) };

        return new CursorLoader(getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.ARTIST_ID + "=?",
                whereVars,
                MediaStore.Audio.Albums.FIRST_YEAR);
    }

    public static long getId(Cursor cursor) {
        return cursor.getLong(0);
    }

    public static String getName(Cursor cursor) {
        return cursor.getString(1);
    }

    public static int getSongCount(Cursor cursor) {
        return cursor.getInt(2);
    }

    public static int getYear(Cursor cursor) {
        return cursor.getInt(3);
    }

    public static String getAlbumArtPath(Cursor cursor) {
        return cursor.getString(4);
    }
}
