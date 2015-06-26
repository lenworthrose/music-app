package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.lenworthrose.music.helper.GridViewHelper;
import com.lenworthrose.music.view.GridItem;

/**
 * Created by Lenny on 2015-06-25.
 */
public class AlbumsAdapter extends CursorAdapter {
    private GridViewHelper helper = new GridViewHelper() {
        @Override
        public void configure(GridItem item, Cursor cursor) {
            item.setText(cursor.getString(2));
            item.setImage(BitmapFactory.decodeFile(cursor.getString(5)));
        }
    };

    public AlbumsAdapter(Context context) {
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
