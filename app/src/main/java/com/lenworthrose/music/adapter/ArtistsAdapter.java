package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;

import com.bumptech.glide.Glide;
import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.sync.ArtistsStore;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

import java.util.ArrayList;

/**
 * A {@link BaseSwitchableAdapter} that manages lists of Artists.
 */
public class ArtistsAdapter extends BaseSwitchableAdapter {
    public ArtistsAdapter(Context context, boolean isGrid) {
        super(context, isGrid);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext()) {
            @Override
            public Cursor loadInBackground() {
                return ArtistsStore.getInstance().getArtists(getFilterQuery());
            }
        };
    }

    @Override
    protected void updateListItem(ListItem view, Context context, Cursor cursor) {
        view.setTitle(cursor.getString(1));

        int albumCount = cursor.getInt(2);
        view.setStatus(context.getResources().getQuantityString(R.plurals.num_of_albums, albumCount, albumCount));

        view.setImageVisible(true);
        Glide.with(getContext()).load(cursor.getString(3)).error(R.drawable.logo).fallback(R.drawable.logo).into(view.getImageView());
    }

    @Override
    protected void updateGridItem(GridItem view, Context context, Cursor cursor) {
        view.setText(cursor.getString(1));

        if (TextUtils.isEmpty(cursor.getString(7))) {
            Glide.with(getContext()).load(cursor.getString(4)).fallback(R.drawable.logo).error(R.drawable.logo).into(view.getBigImageView());
            Glide.with(getContext()).load(android.R.color.transparent).into(view.getImageView1());
            Glide.with(getContext()).load(android.R.color.transparent).into(view.getImageView2());
            Glide.with(getContext()).load(android.R.color.transparent).into(view.getImageView3());
            Glide.with(getContext()).load(android.R.color.transparent).into(view.getImageView4());
        } else {
            Glide.with(getContext()).load(android.R.color.transparent).into(view.getBigImageView());
            Glide.with(getContext()).load(cursor.getString(4)).into(view.getImageView1());
            Glide.with(getContext()).load(cursor.getString(5)).into(view.getImageView2());
            Glide.with(getContext()).load(cursor.getString(6)).into(view.getImageView3());
            Glide.with(getContext()).load(cursor.getString(7)).into(view.getImageView4());
        }
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
