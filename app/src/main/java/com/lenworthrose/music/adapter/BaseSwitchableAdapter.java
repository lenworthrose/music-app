package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.lenworthrose.music.util.NavigationListener;
import com.lenworthrose.music.view.GridItem;
import com.lenworthrose.music.view.ListItem;

public abstract class BaseSwitchableAdapter extends CursorAdapter implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    private boolean isGrid;
    private Context context;
    private NavigationListener navListener;

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

    protected abstract void updateListItem(ListItem view, Context context, Cursor cursor);
    protected abstract void updateGridItem(GridItem view, Context context, Cursor cursor);

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swapCursor(null);
    }
}
