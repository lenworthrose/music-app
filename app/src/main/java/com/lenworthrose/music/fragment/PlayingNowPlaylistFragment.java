package com.lenworthrose.music.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.PlayingNowPlaylistAdapter;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Utils;
import com.lenworthrose.music.playback.PlaybackService;
import com.mobeta.android.dslv.DragSortListView;

/**
 * This Fragment displays the current Playing Now Playlist.
 */
public class PlayingNowPlaylistFragment extends Fragment implements AdapterView.OnItemClickListener, ServiceConnection {
    private DragSortListView listView;
    private PlayingNowPlaylistAdapter adapter;
    private PlaybackService playbackService;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.PLAYING_NOW_CHANGED:
                    playingItemChanged();
                    break;
                case Constants.PLAYING_NOW_PLAYLIST_CHANGED:
                    playlistUpdated();
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setHasOptionsMenu(true);
    }

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
    public void onStart() {
        super.onStart();
        getActivity().bindService(new Intent(getActivity(), PlaybackService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, Utils.createPlaybackIntentFilter());
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        getActivity().unbindService(this);
        super.onStop();
    }

    public void playlistUpdated() {
        if (isDetached()) return;

        adapter.changeCursor(playbackService.getPlaylist());
        playingItemChanged();
    }

    public void playingItemChanged() {
        if (!isDetached() && !playbackService.isPlaylistEmpty()) {
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setItemChecked(playbackService.getPlaylistPosition(), true);
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
        adapter = new PlayingNowPlaylistAdapter(getActivity(), playbackService.getPlaylist());
        listView.setAdapter(adapter);
        playlistUpdated();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }
}

