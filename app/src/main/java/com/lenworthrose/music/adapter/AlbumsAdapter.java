package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;

import com.bumptech.glide.Glide;
import com.lenworthrose.music.IdType;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

import java.util.ArrayList;

/**
 * A {@link BaseSwitchableAdapter} that manages lists of Albums.
 */
public class AlbumsAdapter extends BaseSwitchableAdapter {
    private static String[] PROJECTION = {
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR,
            MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ARTIST
    };

    private IdType type;
    private long parentId;

    public AlbumsAdapter(Context context, boolean isGrid) {
        this(context, isGrid, null, Long.MIN_VALUE);
    }

    public AlbumsAdapter(Context context, boolean isGrid, IdType type, long parentId) {
        super(context, isGrid);
        this.type = type;
        this.parentId = parentId;
    }

    public static CursorLoader getAlbums(Context context, IdType type, long id) {
        String where;
        String[] whereVars = { String.valueOf(id) };

        switch (type) {
            case ARTIST:
                where = MediaStore.Audio.Media.ARTIST_ID;
                break;
            case GENRE:
                //TODO: How do I get a list of albums from a genre? MediaStore maps individual files to genres...
                where = null;
                whereVars = null;
                break;
            case ALBUM:
                where = MediaStore.Audio.Media._ID;
                break;
            default:
                where = null;
                whereVars = null;
                break;
        }

        if (where != null) where += "= ?";

        return new CursorLoader(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                PROJECTION,
                where,
                whereVars,
                MediaStore.Audio.Albums.FIRST_YEAR);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return getAlbums(getContext(), type, parentId);
    }

    @Override
    protected void updateListItem(ListItem view, Context context, Cursor cursor) {
        view.setTitle(cursor.getString(1));
        Glide.with(context).load(cursor.getString(4)).into(view.getImageView());
    }

    @Override
    protected void updateGridItem(GridItem view, Context context, Cursor cursor) {
        view.setText(cursor.getString(1));
        Glide.with(context).load(cursor.getString(4)).into(view.getImageView());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getNavigationListener().onNavigate(IdType.ALBUM, id);
    }

    @Override
    protected void onPlayClicked(ArrayList<Long> ids) {
        getNavigationListener().play(IdType.ALBUM, ids);
    }

    @Override
    protected void onAddClicked(ArrayList<Long> ids) {
        getNavigationListener().add(IdType.ALBUM, ids);
    }

    @Override
    protected void onAddAsNextClicked(ArrayList<Long> ids) {
        getNavigationListener().addAsNext(IdType.ALBUM, ids);
    }
}
