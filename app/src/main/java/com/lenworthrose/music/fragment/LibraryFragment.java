package com.lenworthrose.music.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.AdapterFactory;
import com.lenworthrose.music.adapter.BaseSwitchableAdapter;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.NavigationListener;
import com.lenworthrose.music.view.HeaderGridView;
import com.lenworthrose.music.view.ListHeader;

/**
 * This Fragment handles the display of information from the media library. It can be configured
 * to display content as either a list or a grid.
 * <p>
 * The {@code isGridView()} method is currently used to determine whether the current list should
 * be displayed as a grid or a list. Eventually, this should be determined by user preference, but
 * for now it's hardcoded.
 */
public class LibraryFragment extends Fragment {
    private static String SETTING_VIEW_MODE_ARTISTS = "ArtistsViewMode";
    private static String SETTING_VIEW_MODE_ALBUMS = "AlbumsViewMode";
    private static String SETTING_VIEW_MODE_SONGS = "SongsViewMode";

    private AbsListView absListView;
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

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(isGridView() ? R.layout.grid_view : R.layout.list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        absListView = (AbsListView)view.findViewById(R.id.abs_list_view);

        if (id != Constants.ALL) {
            ListHeader header = new ListHeader(getActivity(), idType, id);

            if (absListView instanceof HeaderGridView)
                ((HeaderGridView)absListView).addHeaderView(header, null, false);
            else
                ((ListView)absListView).addHeaderView(header, null, false);

            if (adapter instanceof ListHeader.ImageLoadListener) header.setImageLoadListener(((ListHeader.ImageLoadListener)adapter));
        }

        absListView.setAdapter(adapter);
        absListView.setOnItemClickListener(adapter);
        absListView.setMultiChoiceModeListener(adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_library_fragment, menu);

        menu.findItem(R.id.action_toggle_view_mode).setIcon(isGridView() ? R.drawable.playlists : R.drawable.grid);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle_view_mode:
                toggleViewMode();
                ((NavigationListener)getActivity()).onViewModeToggled(idType, id);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        absListView.setAdapter(null);
        super.onDestroyView();
    }

    private void toggleViewMode() {
        String toEdit = null;

        if (id == Constants.ALL) {
            switch (idType) {
                case ARTIST:
                    toEdit = SETTING_VIEW_MODE_ARTISTS;
                    break;
                case ALBUM:
                    toEdit = SETTING_VIEW_MODE_ALBUMS;
                    break;
                case SONG:
                    toEdit = SETTING_VIEW_MODE_SONGS;
                    break;
            }
        } else {
            switch (idType) {
                case ALBUM:
                    toEdit = SETTING_VIEW_MODE_SONGS;
                    break;
                case ARTIST:
                    toEdit = SETTING_VIEW_MODE_ALBUMS;
                    break;
                default:
                    toEdit = null;
                    break;
            }
        }
        if (toEdit != null)
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(toEdit, !(absListView instanceof HeaderGridView)).commit();
    }

    private boolean isGridView() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (id == Constants.ALL) {
            switch (idType) {
                case ARTIST:
                    return prefs.getBoolean(SETTING_VIEW_MODE_ARTISTS, true);
                case ALBUM:
                    return prefs.getBoolean(SETTING_VIEW_MODE_ALBUMS, true);
                case SONG:
                    return prefs.getBoolean(SETTING_VIEW_MODE_SONGS, false);
            }
        } else {
            switch (idType) {
                case PLAYLIST:
                    return false;
                case ALBUM:
                    return prefs.getBoolean(SETTING_VIEW_MODE_SONGS, false);
                case ARTIST:
                    return prefs.getBoolean(SETTING_VIEW_MODE_ALBUMS, true);
            }
        }

        return true;
    }

    public static LibraryFragment createRootInstance(IdType type) {
        return createInstance(type, Constants.ALL);
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
