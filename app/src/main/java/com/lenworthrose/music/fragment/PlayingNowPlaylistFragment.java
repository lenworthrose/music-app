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
import com.lenworthrose.music.adapter.PlayingNowAdapter;
import com.lenworthrose.music.helper.Constants;
import com.lenworthrose.music.helper.Utils;
import com.lenworthrose.music.playback.PlaybackService;
import com.mobeta.android.dslv.DragSortListView;

public class PlayingNowPlaylistFragment extends Fragment implements AdapterView.OnItemClickListener, ServiceConnection {
    private DragSortListView listView;
    private PlayingNowAdapter adapter;
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

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        if (menu.findItem(R.id.action_shuffle) == null) {
//            inflater.inflate(R.menu.playlist, menu);
//
//            if (Playback.getInstance().isSavePlaylistSupported())
//                menu.add(R.string.save_playlist).setIcon(R.drawable.save_playlist);
//        }
//
//        super.onCreateOptionsMenu(menu, inflater);
//    }

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

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (!isVisible()) return false;
//
//        switch (item.getItemId()) {
//            case R.id.action_repeat:
//                RepeatDialogFragment repeat = new RepeatDialogFragment();
//                repeat.show(getActivity().getFragmentManager(), "repeatDialog");
//                return true;
//            case R.id.action_shuffle:
//                ShuffleDialogFragment shuffle = new ShuffleDialogFragment();
//                shuffle.show(getActivity().getFragmentManager(), "shuffleDialog");
//                return true;
//            case R.id.action_edit_playlist:
//                if (adapter.getCount() > 0) {
//                    Intent intent = new Intent(getActivity(), EditPlaylistActivity.class);
//                    intent.putExtra(EditPlaylistActivity.SELECTED_ITEM, listView.getCheckedItemPosition());
//                    startActivity(intent);
//                } else {
//                    RemoteApp.showToast(R.string.no_tracks_in_playlist);
//                }
//
//                return true;
//            case 666666:
//                NewPlaylistNameDialogFragment newPlaylist = new NewPlaylistNameDialogFragment();
//                newPlaylist.setTargetFragment(this, 666666);
//                newPlaylist.show(getChildFragmentManager(), null);
//                return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

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

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == 666666 && resultCode == Activity.RESULT_OK) {
//            String name = data.getStringExtra(NewPlaylistNameDialogFragment.NAME);
//
//            BooleanRequest req = new BooleanRequest(Command.SavePlayingNowToPlaylist, name, null, new Response.Listener<Boolean>() {
//                @Override
//                public void onResponse(Boolean response) {
//                    if (response)
//                        RemoteApp.showToast(R.string.playlist_save_successful);
//                    else
//                        RemoteApp.showToast(R.string.playlist_save_failed);
//                }
//            }, new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    RemoteApp.showToast(getString(R.string.error_occurred, Utils.getVolleyErrorString(error)));
//                }
//            });
//
//            RemoteApp.getRequestQueue().add(req);
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playbackService.play(position);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder)service;
        playbackService = binder.getService();
        adapter = new PlayingNowAdapter(getActivity(), playbackService.getPlaylist());
        listView.setAdapter(adapter);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }
}

