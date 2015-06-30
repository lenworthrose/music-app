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

/**
 * This Fragment handles the display of information from the media library. It can be configured
 * to display content as either a list or a grid.
 *
 * The {@code isGridView()} method is currently used to determine whether the current list should
 * be displayed as a grid or a list. Eventually, this should be determined by user preference, but
 * for now it's hardcoded.
 */
public class LibraryFragment extends Fragment {
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

        if (idType != null && !isGridView())
            ((ListView)listView).addHeaderView(new ListHeader(getActivity(), idType, id), null, false);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(adapter);
        listView.setMultiChoiceModeListener(adapter);
    }

    @Override
    public void onDestroyView() {
        if (!isGridView()) ((AbsListView)getView().findViewById(R.id.abs_list_view)).setAdapter(null);
        super.onDestroyView();
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

    public static LibraryFragment createRootInstance() {
        return createInstance(null, -1);
    }

    public static LibraryFragment createInstance(IdType type, long id) {
        Bundle b = new Bundle();
        b.putSerializable(Constants.TYPE, type);
        b.putLong(Constants.ID, id);

        LibraryFragment fragment = new LibraryFragment();
        fragment.setArguments(b);
        return fragment;
    }
}
