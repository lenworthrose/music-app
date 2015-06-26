package com.lenworthrose.music.helper;

import android.database.Cursor;

import com.lenworthrose.music.view.GridItem;

public abstract class GridViewHelper {
    public abstract void configure(GridItem item, Cursor cursor);
}
