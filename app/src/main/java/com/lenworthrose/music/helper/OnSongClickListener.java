package com.lenworthrose.music.helper;

import android.view.View;
import android.widget.AdapterView;

import com.lenworthrose.music.playback.PlaybackDelegator;

public class OnSongClickListener implements AdapterView.OnItemClickListener {
    private long albumId;

    public OnSongClickListener(long albumId) {
        this.albumId = albumId;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlaybackDelegator.playAlbum(parent.getContext(), albumId, position);
    }
}
