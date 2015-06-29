package com.lenworthrose.music.util;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.lenworthrose.music.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lenny on 2015-06-26.
 */
public class MultiSelectListener implements AbsListView.MultiChoiceModeListener {
    private List<Long> ids;
    private String listType;

    public MultiSelectListener(String listType) {
        this.listType = listType;
    }

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
            ids.remove(Long.valueOf(id));
        else
            ids.add(id);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_play:
                break;
            case R.id.action_add:
                break;
            case R.id.action_add_as_next:
                break;
        }

        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        ids = null;
    }
}
