package com.lenworthrose.music.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

public abstract class LoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
    private Context context;
    private CursorAdapter adapter;

    public LoaderCallbacks(Context context, CursorAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
    }

    public Context getContext() {
        return context;
    }

    public CursorAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
}
