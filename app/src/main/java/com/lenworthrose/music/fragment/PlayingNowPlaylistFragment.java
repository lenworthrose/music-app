package com.lenworthrose.music.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.PlayingNowPlaylistAdapter;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Utils;
import com.mobeta.android.dslv.DragSortListView;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * This Fragment displays the current Playing Now Playlist.
 */
public class PlayingNowPlaylistFragment extends Fragment implements AdapterView.OnItemClickListener, ServiceConnection {
    private DragSortListView listView;
    private PlayingNowPlaylistAdapter adapter;
    private PlaybackService playbackService;
    private int currentPlaylistPosition;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.PLAYING_NOW_CHANGED:
                    currentPlaylistPosition = intent.getIntExtra(Constants.EXTRA_PLAYLIST_POSITION, 0) - 1;
                    playingItemChanged();
                    break;
                case Constants.PLAYING_NOW_PLAYLIST_CHANGED:
                    playlistUpdated();
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing_now_playlist, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = (DragSortListView)view.findViewById(R.id.pnp_list_view);
        listView.setEmptyView(view.findViewById(R.id.pnp_empty_view));
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setDragEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().bindService(new Intent(getActivity(), PlaybackService.class), this, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, Utils.createPlaybackIntentFilter());
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        getActivity().unbindService(this);
        super.onPause();
    }

    public void playlistUpdated() {
        if (isDetached() || playbackService == null) return;

        adapter.changeCursor(playbackService.getPlaylist());
        playingItemChanged();
    }

    public void playingItemChanged() {
        if (!isDetached() && adapter != null && !adapter.isEmpty()) {
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setItemChecked(currentPlaylistPosition, true);
                    listView.setSelection(listView.getCheckedItemPosition());
                }
            });
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playbackService.play(position);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder)service;
        playbackService = binder.getService();
        currentPlaylistPosition = playbackService.getPlaylistPosition();
        adapter = new PlayingNowPlaylistAdapter(getActivity(), playbackService.getPlaylist());
        listView.setAdapter(adapter);
        playlistUpdated();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }
}

