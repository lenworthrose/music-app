package com.lenworthrose.music.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.SongsAdapter;
import com.lenworthrose.music.helper.Constants;
import com.lenworthrose.music.helper.MultiSelectListener;
import com.lenworthrose.music.helper.NavigationListener;
import com.lenworthrose.music.helper.OnSongClickListener;
import com.lenworthrose.music.loader.LoaderCallbacks;
import com.lenworthrose.music.loader.SongLoaderCallbacks;

public class ListFragment extends Fragment {
    private CursorAdapter adapter;
    private LoaderCallbacks callbacks;
    private AdapterView.OnItemClickListener clickListener;
    private String type;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!(getActivity() instanceof NavigationListener))
            throw new IllegalStateException("GridFragment's Activity must implement NavigationListener");

//        NavigationListener navListener = (NavigationListener)getActivity();

        if (savedInstanceState == null) savedInstanceState = getArguments();
        type = savedInstanceState.getString(Constants.TYPE);
        long albumId = savedInstanceState.getLong(Constants.ID);

        switch (type) {
            case Constants.TYPE_SONGS:
                adapter = new SongsAdapter(getActivity(), false);
                callbacks = new SongLoaderCallbacks(albumId, getActivity(), adapter);
                clickListener = new OnSongClickListener(albumId);
                break;
        }

        getLoaderManager().initLoader(0, null, callbacks);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = (ListView)view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(clickListener);
        listView.setMultiChoiceModeListener(new MultiSelectListener(type));
    }

    public static ListFragment songsInstance(long albumId) {
        Bundle b = new Bundle();
        b.putString(Constants.TYPE, Constants.TYPE_SONGS);
        b.putLong(Constants.ID, albumId);

        ListFragment fragment = new ListFragment();
        fragment.setArguments(b);
        return fragment;
    }
}
