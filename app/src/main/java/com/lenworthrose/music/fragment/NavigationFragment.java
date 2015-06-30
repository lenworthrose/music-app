package com.lenworthrose.music.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.AdapterFactory;
import com.lenworthrose.music.adapter.BaseSwitchableAdapter;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.view.ListHeader;

public class NavigationFragment extends Fragment {
    private BaseSwitchableAdapter adapter;
    private IdType idType;
    private long id;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) savedInstanceState = getArguments();
        idType = (IdType)savedInstanceState.getSerializable(Constants.TYPE);
        id = savedInstanceState.getLong(Constants.ID);
        adapter = AdapterFactory.createAdapter(getActivity(), isGridView(), idType, id);
        getLoaderManager().initLoader(0, null, adapter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(isGridView() ? R.layout.grid_view : R.layout.list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AbsListView listView = (AbsListView)view.findViewById(R.id.abs_list_view);

        if (idType != null && !isGridView()) {
            ListHeader header = new ListHeader(getActivity(), idType, id);
            ((ListView)listView).addHeaderView(header);
        }

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(adapter);
        listView.setMultiChoiceModeListener(adapter);
    }

    private boolean isGridView() {
        if (idType == null) return true; //Home screen

        switch (idType) {
            case PLAYLIST:
            case ALBUM:
                return false;
            default:
                return true;
        }
    }

    public static NavigationFragment createRootInstance() {
        return createInstance(null, -1);
    }

    public static NavigationFragment createInstance(IdType type, long id) {
        Bundle b = new Bundle();
        b.putSerializable(Constants.TYPE, type);
        b.putLong(Constants.ID, id);

        NavigationFragment fragment = new NavigationFragment();
        fragment.setArguments(b);
        return fragment;
    }
}
