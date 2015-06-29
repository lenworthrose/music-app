package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;

import com.lenworthrose.music.util.ImageLoader;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

import java.util.ArrayList;

public class AlbumsAdapter extends BaseSwitchableAdapter {
    private long parentId;

    public AlbumsAdapter(Context context, boolean isGrid) {
        this(context, isGrid, Long.MIN_VALUE);
    }

    public AlbumsAdapter(Context context, boolean isGrid, long parentId) {
        super(context, isGrid);
        this.parentId = parentId;
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

        String[] whereVars = { String.valueOf(parentId) };

        return new CursorLoader(getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                parentId == Long.MIN_VALUE ? null : MediaStore.Audio.Media.ARTIST_ID + "=?",
                parentId == Long.MIN_VALUE ? null : whereVars,
                MediaStore.Audio.Albums.FIRST_YEAR);
    }

    @Override
    protected void updateListItem(ListItem view, Context context, Cursor cursor) {
        view.setTitle(cursor.getString(1));
        ImageLoader.getInstance().loadImage(cursor.getString(4), view);
    }

    @Override
    protected void updateGridItem(GridItem view, Context context, Cursor cursor) {
        view.setText(cursor.getString(1));
        ImageLoader.getInstance().loadImage(cursor.getString(4), view);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getNavigationListener().onNavigateToAlbum(id);
    }

    @Override
    protected void onPlayClicked(ArrayList<Long> ids) {
        getNavigationListener().playAlbums(ids);
    }

    @Override
    protected void onAddClicked(ArrayList<Long> ids) {
        getNavigationListener().addAlbums(ids);
    }

    @Override
    protected void onAddAsNextClicked(ArrayList<Long> ids) {
        getNavigationListener().addAlbumsAsNext(ids);
    }
}
