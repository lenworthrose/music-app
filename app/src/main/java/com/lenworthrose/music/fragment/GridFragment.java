package com.lenworthrose.music.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.AlbumsAdapter;
import com.lenworthrose.music.adapter.ArtistsAdapter;
import com.lenworthrose.music.helper.Constants;
import com.lenworthrose.music.helper.MultiSelectListener;
import com.lenworthrose.music.helper.NavigationListener;
import com.lenworthrose.music.helper.OnAlbumClickListener;
import com.lenworthrose.music.helper.OnArtistClickListener;
import com.lenworthrose.music.loader.AlbumLoaderCallbacks;
import com.lenworthrose.music.loader.ArtistLoaderCallbacks;
import com.lenworthrose.music.loader.LoaderCallbacks;

public class GridFragment extends Fragment {
    private String type;
    private CursorAdapter adapter;
    private LoaderCallbacks callbacks;
    private AdapterView.OnItemClickListener clickListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!(getActivity() instanceof NavigationListener))
            throw new IllegalStateException("GridFragment's Activity must implement NavigationListener");

        NavigationListener navListener = (NavigationListener)getActivity();

        if (savedInstanceState == null) savedInstanceState = getArguments();
        type = savedInstanceState.getString(Constants.TYPE);

        switch (type) {
            case Constants.TYPE_ARTISTS:
                adapter = new ArtistsAdapter(getActivity());
                callbacks = new ArtistLoaderCallbacks(getActivity(), adapter);
                clickListener = new OnArtistClickListener(navListener);
                break;
            case Constants.TYPE_ALBUMS:
                adapter = new AlbumsAdapter(getActivity());
                callbacks = new AlbumLoaderCallbacks(savedInstanceState.getLong(Constants.ID), getActivity(), adapter);
                clickListener = new OnAlbumClickListener(navListener);
                break;
            case Constants.TYPE_SONGS:
                break;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.grid_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridView grid = (GridView)view.findViewById(R.id.grid_view);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(clickListener);
        grid.setMultiChoiceModeListener(new MultiSelectListener(type));

        getLoaderManager().initLoader(0, null, callbacks);
    }

    public static GridFragment artistsInstance() {
        Bundle b = new Bundle();
        b.putString(Constants.TYPE, Constants.TYPE_ARTISTS);

        GridFragment fragment = new GridFragment();
        fragment.setArguments(b);
        return fragment;
    }

    public static GridFragment albumsInstance(long artistId) {
        Bundle b = new Bundle();
        b.putString(Constants.TYPE, Constants.TYPE_ALBUMS);
        b.putLong(Constants.ID, artistId);

        GridFragment fragment = new GridFragment();
        fragment.setArguments(b);
        return fragment;
    }
}
