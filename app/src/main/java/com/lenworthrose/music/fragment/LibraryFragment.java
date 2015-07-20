package com.lenworthrose.music.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
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
import com.lenworthrose.music.sync.MediaStoreSyncService;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.NavigationListener;
import com.lenworthrose.music.view.HeaderGridView;
import com.lenworthrose.music.view.ListHeader;

/**
 * This Fragment handles the display of information from the media library. It can be configured
 * to display content as either a list or a grid.
 * <p/>
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
    private SearchView searchView;
    private String filter;
    private ListHeader listHeader;
    private BroadcastReceiver databaseChangeReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) savedInstanceState = getArguments();
        idType = (IdType)savedInstanceState.getSerializable(Constants.TYPE);
        id = savedInstanceState.getLong(Constants.ID);

        if (adapter == null) {
            adapter = AdapterFactory.createAdapter(getActivity(), isGridView(), idType, id);
            getLoaderManager().initLoader(0, null, adapter);
        }

        databaseChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getLoaderManager().restartLoader(0, null, adapter);
            }
        };

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
            if (listHeader == null)
                listHeader = new ListHeader(getActivity(), idType, id);
            else if (listHeader.getParent() != null)
                ((ViewGroup)listHeader.getParent()).removeView(listHeader);

            if (absListView instanceof HeaderGridView)
                ((HeaderGridView)absListView).addHeaderView(listHeader, null, false);
            else
                ((ListView)absListView).addHeaderView(listHeader, null, false);
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
        searchView = (SearchView)menu.findItem(R.id.action_filter).getActionView();
        searchView.setQueryHint(getString(R.string.filter_hint_text));

        if (!TextUtils.isEmpty(filter)) {
            searchView.setQuery(filter, false);
            searchView.setIconified(false);
            searchView.clearFocus();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    searchView.setIconified(true);
                    adapter.setFilter(null);
                } else {
                    adapter.setFilter(newText);
                }

                getLoaderManager().restartLoader(0, null, adapter);
                return true;
            }
        });

        //Without this onCloseListener, the keyboard will appear after closing an inactive SearchView
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchView.post(new Runnable() {
                    @Override
                    public void run() {
                        searchView.clearFocus();
                    }
                });

                return false;
            }
        });
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
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MediaStoreSyncService.ACTION_MEDIA_STORE_SYNC_COMPLETE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(databaseChangeReceiver, filter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(databaseChangeReceiver);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (searchView != null) {
            filter = searchView.getQuery().toString();
            searchView.clearFocus();
        }

        absListView.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.TYPE, idType);
        outState.putLong(Constants.ID, id);
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
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(toEdit, !(absListView instanceof HeaderGridView)).apply();
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
