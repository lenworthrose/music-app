package com.lenworthrose.music.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.SearchAdapter;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

/**
 * Fragment for performing a search of the media library.
 */
public class SearchFragment extends Fragment {
    private SearchAdapter adapter;
    private SearchView searchView;
    private String query;
    private Handler handler;
    private View loadingSpinner;

    private SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            handler.removeCallbacksAndMessages(null);
            SearchFragment.this.query = query;
            adapter.setQuery(query);
            searchView.clearFocus();
            hideLoadingSpinner();
            return true;
        }

        @Override
        public boolean onQueryTextChange(final String newText) {
            handler.removeCallbacksAndMessages(null);

            if (!TextUtils.isEmpty(newText)) {
                if (loadingSpinner.getVisibility() != View.VISIBLE)
                    showLoadingSpinner();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setQuery(newText);
                        hideLoadingSpinner();
                    }
                }, 400);
            }

            return true;
        }
    };

    private SearchView.OnCloseListener searchCloseListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            searchView.setQuery(null, false);
            return true;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
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

        loadingSpinner = view.findViewById(R.id.search_loading_spinner);
        StickyGridHeadersGridView gridView = (StickyGridHeadersGridView)view.findViewById(R.id.search_grid_view);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(adapter);
        gridView.setOnCreateContextMenuListener(adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search_fragment, menu);

        searchView = (SearchView)menu.findItem(R.id.action_search).getActionView();
        searchView.setQueryHint(getString(R.string.search_hint_text));
        searchView.setIconified(false);

        if (query != null) {
            searchView.setQuery(query, false);
            searchView.clearFocus();
        }

        searchView.setOnQueryTextListener(queryTextListener);
        searchView.setOnCloseListener(searchCloseListener);
    }

    @Override
    public void onDestroyView() {
        if (searchView != null) {
            query = searchView.getQuery().toString();
            searchView.clearFocus();
        }

        super.onDestroyView();
    }

    private void hideLoadingSpinner() {
        loadingSpinner.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
            @Override
            public void run() {
                loadingSpinner.setVisibility(View.GONE);
            }
        });
    }

    private void showLoadingSpinner() {
        loadingSpinner.animate().alpha(1f).setDuration(150).withStartAction(new Runnable() {
            @Override
            public void run() {
                loadingSpinner.setAlpha(0f);
                loadingSpinner.setVisibility(View.VISIBLE);
            }
        });
    }
}
