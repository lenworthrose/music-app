package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.lenworthrose.music.helper.Utils;
import com.lenworthrose.music.loader.SongLoaderCallbacks;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

/**
 * Created by Lenny on 2015-06-26.
 */
public class SongsAdapter extends CursorAdapter {
    private boolean isGrid;

    public SongsAdapter(Context context, boolean isGrid) {
        super(context, null, 0);
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
        String title = SongLoaderCallbacks.getName(cursor);
        int track = SongLoaderCallbacks.getTrackNum(cursor) % 1000;
        String toShow = String.valueOf(track) + ". " + title;

        if (isGrid) {
            GridItem item = (GridItem)view;
            item.setText(toShow);
        } else {
            ListItem item = (ListItem)view;
            item.setTitle(toShow);
            item.setStatus(Utils.longToTimeDisplay(SongLoaderCallbacks.getDuration(cursor)));
        }
    }
}
