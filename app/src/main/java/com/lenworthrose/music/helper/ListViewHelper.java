package com.lenworthrose.music.helper;

import android.database.Cursor;

import com.lenworthrose.music.view.ListItem;

public abstract class ListViewHelper {
    public abstract void configure(ListItem item, Cursor cursor);
}
