package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.sync.ArtistsStore;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.NavigationListener;
import com.lenworthrose.music.util.Utils;
import com.lenworthrose.music.view.GridItem;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleAdapter;

import java.util.ArrayList;

/**
 * Adapter for use in the {@link com.lenworthrose.music.fragment.SearchFragment}.
 */
public class SearchAdapter extends BaseAdapter implements StickyGridHeadersSimpleAdapter, LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {
    private static final int ARTISTS_ID = 0, ALBUMS_ID = 1, SONGS_ID = 2;

    private Cursor[] cursors;
    private String query;
    private Context context;
    private LoaderManager loaderManager;

    public SearchAdapter(Context context, LoaderManager loaderManager) {
        this.context = context;
        this.loaderManager = loaderManager;
        cursors = new Cursor[3]; // Artists, Albums, Songs
    }

    public void setQuery(String query) {
        this.query = query;
        loaderManager.restartLoader(ARTISTS_ID, null, this);
        loaderManager.restartLoader(ALBUMS_ID, null, this);
        loaderManager.restartLoader(SONGS_ID, null, this);
    }

    private int getCursorForPosition(int position) {
        if (position < 0) return -1;

        int currentTotal = 0;

        for (int i = 0; i < cursors.length; i++) {
            Cursor cursor = cursors[i];
            if (cursor == null) continue;

            currentTotal += cursor.getCount();

            if (position < currentTotal) return i;
        }

        return -1;
    }

    @Override
    public long getHeaderId(int position) {
        return getCursorForPosition(position);
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.search_grid_header, parent, false);
            convertView.setTag(R.id.search_header_text, convertView.findViewById(R.id.search_header_text));
        }

        String[] titles = context.getResources().getStringArray(R.array.start_locations);
        ((TextView)convertView.getTag(R.id.search_header_text)).setText(titles[getCursorForPosition(position)]);
        return convertView;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        int retVal = 0;

        for (Cursor cursor : cursors)
            if (cursor != null)
                retVal += cursor.getCount();

        return retVal;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return getRecordForPosition(position).getLong(0);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private Cursor getRecordForPosition(int position) {
        return getRecordForPosition(position, getCursorForPosition(position));
    }

    private Cursor getRecordForPosition(int position, int curId) {
        Cursor cursor = cursors[curId];
        cursor.moveToPosition(getPositionInCursor(position, curId));
        return cursor;
    }

    private int getPositionInCursor(int position, int curId) {
        int curTotal = 0;

        for (int i = 0; i < curId; i++)
            curTotal += cursors[i] == null ? 0 : cursors[i].getCount();

        return position - curTotal;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GridItem item = (GridItem)convertView;
        if (item == null) item = new GridItem(context);

        int curId = getCursorForPosition(position);
        Cursor cursor = getRecordForPosition(position, curId);
        String imageUrl = null;

        switch (curId) {
            case ARTISTS_ID:
                item.setText(cursor.getString(2));
                imageUrl = cursor.getString(4);
                break;
            case ALBUMS_ID:
                item.setText(cursor.getString(1));
                imageUrl = cursor.getString(4);
                break;
            case SONGS_ID:
                item.setText(cursor.getString(1));
                imageUrl = Utils.buildAlbumArtUrl(cursor.getLong(6));
                break;
        }

        Glide.with(context).load(imageUrl).into(item.getImageView());

        return item;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ARTISTS_ID:
                return new CursorLoader(context) {
                    @Override
                    public Cursor loadInBackground() {
                        return ArtistsStore.getInstance().getArtists(query);
                    }
                };
            case ALBUMS_ID:
                return AlbumsAdapter.getAlbums(context, null, Constants.ALL, query);
            case SONGS_ID:
                return SongsAdapter.createSongsLoader(context, null, Constants.ALL, query);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursors[loader.getId()] = data;
        notifyDataSetInvalidated();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursors[loader.getId()] = null;
        notifyDataSetInvalidated();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        NavigationListener navListener = (NavigationListener)context;
        int curId = getCursorForPosition(position);

        switch (curId) {
            case ARTISTS_ID:
                navListener.onNavigate(IdType.ARTIST, id);
                break;
            case ALBUMS_ID:
                navListener.onNavigate(IdType.ALBUM, id);
                break;
            case SONGS_ID:
                ArrayList<Long> temp = new ArrayList<>(1);
                temp.add(id);
                navListener.play(IdType.SONG, temp);
                break;
        }
    }
}
