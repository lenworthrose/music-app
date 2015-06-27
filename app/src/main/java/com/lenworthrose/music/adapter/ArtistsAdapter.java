package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.lenworthrose.music.R;
import com.lenworthrose.music.loader.ArtistLoaderCallbacks;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

public class ArtistsAdapter extends CursorAdapter {
    private boolean isGrid;
    private Context context;

    public ArtistsAdapter(Context context, boolean isGrid) {
        super(context, null, 0);
        this.context = context;
        this.isGrid = isGrid;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (isGrid)
            return new GridItem(context);
        else
            return new ListItem(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (isGrid) {
            GridItem item = (GridItem)view;
            item.setText(ArtistLoaderCallbacks.getName(cursor));
        } else {
            ListItem item = (ListItem)view;
            item.setTitle(ArtistLoaderCallbacks.getName(cursor));

            int albumCount = ArtistLoaderCallbacks.getAlbumCount(cursor);
            item.setStatus(context.getResources().getQuantityString(R.plurals.num_of_albums, albumCount, albumCount));
        }
    }
}
