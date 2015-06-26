package com.lenworthrose.music.helper;

import android.view.View;
import android.widget.AdapterView;

/**
 * Created by Lenny on 2015-06-25.
 */
public class OnAlbumClickListener implements AdapterView.OnItemClickListener {
    private NavigationListener listener;

    public OnAlbumClickListener(NavigationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        listener.onNavigateToAlbum(id);
    }
}
