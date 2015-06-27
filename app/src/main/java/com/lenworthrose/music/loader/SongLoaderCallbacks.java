package com.lenworthrose.music.loader;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

public class SongLoaderCallbacks extends LoaderCallbacks {
    private long albumId;

    public SongLoaderCallbacks(long albumId, Context context, CursorAdapter adapter) {
        super(context, adapter);
        this.albumId = albumId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM
        };

        String[] whereVars = { String.valueOf(albumId) };

        return new CursorLoader(getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.ALBUM_ID + "=?",
                whereVars,
                MediaStore.Audio.Media.TRACK);
    }

    public static long getId(Cursor cursor) {
        return cursor.getLong(0);
    }

    public static String getName(Cursor cursor) {
        return cursor.getString(1);
    }

    public static int getTrackNum(Cursor cursor) {
        return cursor.getInt(2);
    }

    public static long getDuration(Cursor cursor) {
        return cursor.getLong(3);
    }

    public static String getArtist(Cursor cursor) {
        return cursor.getString(4);
    }

    public static String getAlbum(Cursor cursor) {
        return cursor.getString(5);
    }
}
