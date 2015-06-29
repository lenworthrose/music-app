package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;

import com.lenworthrose.music.helper.Utils;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

public class SongsAdapter extends BaseSwitchableAdapter {
    private long parentId;

    public SongsAdapter(Context context, boolean isGrid) {
        this(context, isGrid, Long.MIN_VALUE);
    }

    public SongsAdapter(Context context, boolean isGrid, long parentId) {
        super(context, isGrid);
        this.parentId = parentId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String[] whereVars = { String.valueOf(parentId) };

        return new CursorLoader(getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                parentId == Long.MIN_VALUE ? null : MediaStore.Audio.Media.ALBUM_ID + "=?",
                parentId == Long.MIN_VALUE ? null : whereVars,
                MediaStore.Audio.Media.TRACK);
    }

    @Override
    protected void updateListItem(ListItem view, Context context, Cursor cursor) {
        view.setTitle(buildTitle(cursor));
        view.setStatus(Utils.longToTimeDisplay(cursor.getLong(3)));
    }

    @Override
    protected void updateGridItem(GridItem view, Context context, Cursor cursor) {
        view.setText(buildTitle(cursor));
    }

    protected String buildTitle(Cursor cursor) {
        String title = cursor.getString(1);
        int track = cursor.getInt(2) % 1000;
        return track + ". " + title;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        PlaybackDelegator.playAlbum(parent.getContext(), albumId, position);
    }
}
