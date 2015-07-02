package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.util.Utils;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

import java.util.ArrayList;

/**
 * A {@link BaseSwitchableAdapter} that manages lists of Songs.
 */
public class SongsAdapter extends BaseSwitchableAdapter {
    private long parentId;
    private IdType type;

    public SongsAdapter(Context context, boolean isGrid) {
        this(context, isGrid, null, Long.MIN_VALUE);
    }

    public SongsAdapter(Context context, boolean isGrid, IdType type, long parentId) {
        super(context, isGrid);
        this.type = type;
        this.parentId = parentId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        ArrayList<Long> list = new ArrayList<>();
        list.add(parentId);
        return createSongsLoader(getContext(), type, list);
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
        //Subtract difference of counts to account for ListView headers
        getNavigationListener().playSongs(getCursor(), position - (parent.getCount() - getCount()));
    }

    public static CursorLoader createSongsLoader(Context context, IdType type, ArrayList<Long> parentIds) {
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String where = null;
        String[] whereVars = null;
        String sortOrder;

        if (parentIds != null) {
            StringBuilder sb;

            switch (type) {
                case ARTIST:
                    sb = new StringBuilder(MediaStore.Audio.Media.ARTIST_ID);
                    break;
                case ALBUM:
                    sb = new StringBuilder(MediaStore.Audio.Media.ALBUM_ID);
                    break;
                default:
                    return null;
            }

            whereVars = new String[parentIds.size()];

            if (parentIds.size() > 1) {
                sb.append(" IN (");

                for (int i = 0; i < parentIds.size(); i++) {
                    sb.append("?,");
                    whereVars[i] = String.valueOf(parentIds.get(i));
                }

                sb.setLength(sb.length() - 1);
                sb.append(')');
            } else {
                sb.append("= ?");
                whereVars[0] = String.valueOf(parentIds.get(0));
            }

            where = sb.toString();
            sortOrder = MediaStore.Audio.Media.ALBUM_ID + ',' + MediaStore.Audio.Media.TRACK;
        } else {
            sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        }

        return new CursorLoader(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                where,
                whereVars,
                sortOrder);
    }

    @Override
    protected void onPlayClicked(ArrayList<Long> ids) {

    }

    @Override
    protected void onAddClicked(ArrayList<Long> ids) {

    }

    @Override
    protected void onAddAsNextClicked(ArrayList<Long> ids) {

    }
}
