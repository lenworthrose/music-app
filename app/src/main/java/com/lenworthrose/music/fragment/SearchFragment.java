package com.lenworthrose.music.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.SearchAdapter;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

/**
 * Fragment for performing a search of the media library.
 */
public class SearchFragment extends Fragment {
    private SearchAdapter adapter;
    private SearchView searchView;
    private String query;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (adapter == null)
            adapter = new SearchAdapter(getActivity(), getActivity().getSupportLoaderManager());

        StickyGridHeadersGridView gridView = (StickyGridHeadersGridView)view.findViewById(R.id.search_grid_view);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search_fragment, menu);

        searchView = (SearchView)menu.findItem(R.id.action_search).getActionView();
        searchView.setIconified(false);

        if (query != null) {
            searchView.setQuery(query, false);
            searchView.clearFocus();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                SearchFragment.this.query = query;
                adapter.setQuery(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchView.setQuery(null, false);
                return true;
            }
        });
    }
}
