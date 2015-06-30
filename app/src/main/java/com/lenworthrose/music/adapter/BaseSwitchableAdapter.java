package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.util.NavigationListener;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

import java.util.ArrayList;

public abstract class BaseSwitchableAdapter extends CursorAdapter implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>,
        AbsListView.MultiChoiceModeListener {
    private boolean isGrid;
    private Context context;
    private NavigationListener navListener;
    private ArrayList<Long> ids;

    public BaseSwitchableAdapter(Context context, boolean isGrid) {
        super(context, null, 0);

        if (!(context instanceof NavigationListener))
            throw new IllegalArgumentException("Adapter's Context must implement NavigationListener");

        this.context = context;
        this.navListener = (NavigationListener)context;
        this.isGrid = isGrid;
    }

    protected Context getContext() {
        return context;
    }

    protected NavigationListener getNavigationListener() {
        return navListener;
    }

    protected boolean isGrid() {
        return isGrid;
    }

    //--------------CursorAdapter impl

    @Override
    public final View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (isGrid)
            return new GridItem(context);
        else
            return new ListItem(context);
    }

    @Override
    public final void bindView(View view, Context context, Cursor cursor) {
        if (isGrid)
            updateGridItem((GridItem)view, context, cursor);
        else
            updateListItem((ListItem)view, context, cursor);
    }

    //--------------LoaderManager.LoaderCallbacks impl

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swapCursor(null);
    }

    //--------------AbsListView.MultiChoiceModeListener impl

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        ids = new ArrayList<>();
        mode.getMenuInflater().inflate(R.menu.menu_multi_select, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked)
            ids.add(id);
        else
            ids.remove(Long.valueOf(id));

        mode.setTitle(getContext().getString(R.string.num_selected, ids.size()));
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_play:
                onPlayClicked(ids);
                break;
            case R.id.action_add:
                onAddClicked(ids);
                break;
            case R.id.action_add_as_next:
                onAddAsNextClicked(ids);
                break;
        }

        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        ids = null;
    }

    //--------------Abstract methods

    protected abstract void onPlayClicked(ArrayList<Long> ids);
    protected abstract void onAddClicked(ArrayList<Long> ids);
    protected abstract void onAddAsNextClicked(ArrayList<Long> ids);

    protected abstract void updateListItem(ListItem view, Context context, Cursor cursor);
    protected abstract void updateGridItem(GridItem view, Context context, Cursor cursor);
}
