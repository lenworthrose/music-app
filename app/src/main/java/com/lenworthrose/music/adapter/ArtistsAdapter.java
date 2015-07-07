package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;

import com.bumptech.glide.Glide;
import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.sync.ArtistModel;
import com.lenworthrose.music.sync.ArtistsStore;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link BaseSwitchableAdapter} that manages lists of Artists.
 */
public class ArtistsAdapter extends BaseSwitchableAdapter implements ArtistsStore.ArtistsStoreListener {
    private CursorLoader loader;

    public ArtistsAdapter(Context context, boolean isGrid) {
        super(context, isGrid);
        ArtistsStore.getInstance().addListener(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        loader = new CursorLoader(getContext()) {
            @Override
            public Cursor loadInBackground() {
                return ArtistsStore.getInstance().getArtists(getFilterQuery());
            }
        };

        return loader;
    }

    @Override
    public void onMediaStoreSyncComplete(List<ArtistModel> newArtists) {
        loader.forceLoad();
    }

    @Override
    public void onArtistInfoFetchComplete() {
        if (loader != null) loader.forceLoad();
    }

    @Override
    protected void updateListItem(ListItem view, Context context, Cursor cursor) {
        view.setTitle(cursor.getString(2));

        int albumCount = cursor.getInt(3);
        view.setStatus(context.getResources().getQuantityString(R.plurals.num_of_albums, albumCount, albumCount));

        view.setImageVisible(true);
        Glide.with(getContext()).load(cursor.getString(4)).error(R.drawable.logo).fallback(R.drawable.logo).into(view.getImageView());
    }

    @Override
    protected void updateGridItem(GridItem view, Context context, Cursor cursor) {
        view.setText(cursor.getString(2));
        Glide.with(getContext()).load(cursor.getString(5)).error(R.drawable.logo).fallback(R.drawable.logo).into(view.getImageView());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getNavigationListener().onNavigate(IdType.ARTIST, id);
    }

    @Override
    protected void onPlayClicked(ArrayList<Long> ids) {
        getNavigationListener().play(IdType.ARTIST, ids);
    }

    @Override
    protected void onAddClicked(ArrayList<Long> ids) {
        getNavigationListener().add(IdType.ARTIST, ids);
    }

    @Override
    protected void onAddAsNextClicked(ArrayList<Long> ids) {
        getNavigationListener().addAsNext(IdType.ARTIST, ids);
    }
}
