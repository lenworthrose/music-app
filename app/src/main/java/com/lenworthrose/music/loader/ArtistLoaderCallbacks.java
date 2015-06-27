package com.lenworthrose.music.loader;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

public class ArtistLoaderCallbacks extends LoaderCallbacks {
    public ArtistLoaderCallbacks(Context context, CursorAdapter adapter) {
        super(context, adapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS };

        return new CursorLoader(getContext(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
    }

    public static long getId(Cursor cursor) {
        return cursor.getLong(0);
    }

    public static String getName(Cursor cursor) {
        return cursor.getString(1);
    }

    public static int getAlbumCount(Cursor cursor) {
        return cursor.getInt(2);
    }
}
