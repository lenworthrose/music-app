package com.lenworthrose.music.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.AlbumsAdapter;
import com.lenworthrose.music.adapter.ArtistsAdapter;
import com.lenworthrose.music.adapter.BaseSwitchableAdapter;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.MultiSelectListener;

public class GridFragment extends Fragment {
    private String type;
    private BaseSwitchableAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) savedInstanceState = getArguments();
        type = savedInstanceState.getString(Constants.TYPE);

        switch (type) {
            case Constants.TYPE_ARTISTS:
                adapter = new ArtistsAdapter(getActivity(), true);
                break;
            case Constants.TYPE_ALBUMS:
                adapter = new AlbumsAdapter(getActivity(), true, savedInstanceState.getLong(Constants.ID));
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
        grid.setOnItemClickListener(adapter);
        grid.setMultiChoiceModeListener(new MultiSelectListener(type));

        getLoaderManager().initLoader(0, null, adapter);
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
