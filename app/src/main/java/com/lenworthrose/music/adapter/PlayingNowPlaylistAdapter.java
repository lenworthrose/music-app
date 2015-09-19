package com.lenworthrose.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaylistAction;
import com.lenworthrose.music.util.Utils;
import com.lenworthrose.music.view.ListItem;
import com.mobeta.android.dslv.DragSortCursorAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles rendering the Playing Now Playlist. Extends {@link DragSortCursorAdapter} because
 * it will eventually be used to handle playlist editing.
 */
public class PlayingNowPlaylistAdapter extends DragSortCursorAdapter {
    private boolean isEditModeEnabled;
    private List<PlaylistAction> actions;

    public PlayingNowPlaylistAdapter(Context context, Cursor cur) {
        super(context, cur, 0);
        resetEditActions();
    }

    public List<PlaylistAction> getEditActions() {
        return actions;
    }

    public void resetEditActions() {
        actions = new ArrayList<>();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ListItem item = new ListItem(context);
        item.setLayoutParams(parent.getLayoutParams());
        return item;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (cursor.isClosed()) return;

        ListItem item = (ListItem)view;
        item.setTitle((getListPosition(cursor.getPosition()) + 1) + ". " + cursor.getString(5));
        item.setSubtitle(cursor.getString(3));
        item.setStatus(Utils.longToTimeDisplay(cursor.getLong(7)));
        item.setImageVisible(true);
        item.setEditModeEnabled(isEditModeEnabled);
        Glide.with(context).load(cursor.getString(8)).fallback(R.drawable.logo).error(R.drawable.logo).into(item.getImageView());
    }

    public void setEditModeEnabled(boolean enabled) {
        if (isEditModeEnabled != enabled) {
            isEditModeEnabled = enabled;
            notifyDataSetChanged();
        }
    }

    @Override
    public void remove(int which) {
        super.remove(which);
        actions.add(new PlaylistAction(which));
    }

    @Override
    public void drop(int from, int to) {
        super.drop(from, to);
        actions.add(new PlaylistAction(from, to));
    }
}

