package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.lenworthrose.music.loader.AlbumLoaderCallbacks;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

/**
 * Created by Lenny on 2015-06-25.
 */
public class AlbumsAdapter extends CursorAdapter {
    private boolean isGrid;

    public AlbumsAdapter(Context context, boolean isGrid) {
        super(context, null, 0);
        this.isGrid = isGrid;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if(isGrid)
            return new GridItem(context);
        else
            return new ListItem(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (isGrid) {
            GridItem item = (GridItem)view;
            item.setText(AlbumLoaderCallbacks.getName(cursor));
            item.setImage(BitmapFactory.decodeFile(AlbumLoaderCallbacks.getAlbumArtPath(cursor)));
        } else {
            ListItem item = (ListItem)view;
            item.setTitle(AlbumLoaderCallbacks.getName(cursor));
            item.setImage(BitmapFactory.decodeFile(AlbumLoaderCallbacks.getAlbumArtPath(cursor)));
        }
    }
}
