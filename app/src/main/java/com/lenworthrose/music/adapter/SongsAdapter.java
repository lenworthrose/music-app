package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Utils;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListHeader;
import com.lenworthrose.music.view.ListItem;

import java.util.ArrayList;

/**
 * A {@link BaseSwitchableAdapter} that manages lists of Songs.
 */
public class SongsAdapter extends BaseSwitchableAdapter implements ListHeader.ImageLoadListener {
    private static String[] PROJECTION = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID
    };

    private long parentId;
    private IdType type;
    private Bitmap bitmap;

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
        return createSongsLoader(getContext(), type, parentId);
    }

    @Override
    protected void updateListItem(ListItem view, Context context, Cursor cursor) {
        view.setTitle(buildTitle(cursor));
        view.setStatus(Utils.longToTimeDisplay(cursor.getLong(3)));
        if (parentId == Constants.ALL) view.setSubtitle(cursor.getString(4));
    }

    @Override
    protected void updateGridItem(GridItem view, Context context, Cursor cursor) {
        view.setText(buildTitle(cursor));
        view.getImageView().setImageBitmap(bitmap);
    }

    protected String buildTitle(Cursor cursor) {
        String title = cursor.getString(1);

        if (parentId != Constants.ALL) {
            int track = cursor.getInt(2) % 1000;
            return track + ". " + title;
        }

        return title;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Subtract difference of counts to account for ListView headers
        getNavigationListener().playSongs(getCursor(), position - (parent.getCount() - getCount()));
    }

    public static Cursor createSongsCursor(Context context, IdType type, long id) {
        String where = buildWhere(type);
        String[] whereVars = type == null ? null : new String[] { String.valueOf(id) };
        String sortOrder = buildSortOrder(type);

        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                where,
                whereVars,
                sortOrder);
    }

    public static CursorLoader createSongsLoader(Context context, IdType type, long id) {
        String where = buildWhere(type);
        String[] whereVars = type == null ? null : new String[] { String.valueOf(id) };
        String sortOrder = buildSortOrder(type);

        return new CursorLoader(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                where,
                whereVars,
                sortOrder);
    }

    private static String buildWhere(IdType type) {
        if (type != null) {
            String where;

            switch (type) {
                case ARTIST:
                    where = MediaStore.Audio.Media.ARTIST_ID;
                    break;
                case ALBUM:
                    where = MediaStore.Audio.Media.ALBUM_ID;
                    break;
                case SONG:
                    where = MediaStore.Audio.Media._ID;
                    break;
                default:
                    return null;
            }

            return where + "= ?";
        }

        return null;
    }

    private static String buildSortOrder(IdType type) {
        return type == null ? MediaStore.Audio.Media.DEFAULT_SORT_ORDER
                : MediaStore.Audio.Media.ALBUM_ID + ',' + MediaStore.Audio.Media.TRACK;
    }

    @Override
    protected void onPlayClicked(ArrayList<Long> ids) {
        getNavigationListener().play(IdType.SONG, ids);
    }

    @Override
    protected void onAddClicked(ArrayList<Long> ids) {
        getNavigationListener().add(IdType.SONG, ids);
    }

    @Override
    protected void onAddAsNextClicked(ArrayList<Long> ids) {
        getNavigationListener().addAsNext(IdType.SONG, ids);
    }

    @Override
    public void onImageLoaded(Bitmap image) {
        bitmap = image;
        notifyDataSetInvalidated();
    }
}
