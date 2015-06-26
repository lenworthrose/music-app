package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.lenworthrose.music.helper.GridViewHelper;
import com.lenworthrose.music.view.GridItem;

public class ArtistsAdapter extends CursorAdapter {
    private GridViewHelper helper = new GridViewHelper() {
        @Override
        public void configure(GridItem item, Cursor cursor) {
            item.setText(cursor.getString(1));
        }
    };

    public ArtistsAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new GridItem(context, helper);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((GridItem)view).setData(cursor);
    }
}
