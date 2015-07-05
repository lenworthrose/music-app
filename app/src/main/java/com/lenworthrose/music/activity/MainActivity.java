package com.lenworthrose.music.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.fragment.LibraryFragment;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.sync.ArtistsStore;
import com.lenworthrose.music.sync.MediaStoreService;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.NavigationListener;
import com.lenworthrose.music.view.NowPlayingBar;

import java.util.ArrayList;

/**
 * The main Activity for the application. Implements {@link NavigationListener} so it can handle navigation
 * events. Manages the {@link LibraryFragment}s that display the media library.
 * <p/>
 * Responsible for starting and stopping the {@link PlaybackService}. Also binds to the Service so it can
 * modify the Playing Now playlist.
 * <p/>
 * Responsible for starting the {@link MediaStoreService}.
 */
public class MainActivity extends AppCompatActivity implements NavigationListener, ServiceConnection, ArtistsStore.InitListener {
    private PlaybackService playbackService;
    private NowPlayingBar nowPlayingBar;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        nowPlayingBar = (NowPlayingBar)findViewById(R.id.main_now_playing_bar);

        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(Constants.CMD_ACTIVITY_STARTING);
        startService(intent);

        ArtistsStore.getInstance().init(this, this);
        startService(new Intent(this, MediaStoreService.class));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getExtras().getInt(Constants.EXTRA_MODIFICATION_TYPE)) {
                    case Constants.EXTRA_MODIFICATION_TYPE_PLAY:
                        startActivity(new Intent(MainActivity.this, PlayingNowActivity.class));
                        break;
                    case Constants.EXTRA_MODIFICATION_TYPE_ADD:
                        Toast.makeText(MainActivity.this, R.string.added, Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.EXTRA_MODIFICATION_TYPE_ADD_AS_NEXT:
                        Toast.makeText(MainActivity.this, R.string.added_as_next, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_playing_now:
                startActivity(new Intent(this, PlayingNowActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, PlaybackService.class), this, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.PLAYBACK_MODIFICATION_COMPLETE));
        nowPlayingBar.setPlaybackService(playbackService);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        nowPlayingBar.setPlaybackService(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(this);
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(Constants.CMD_ACTIVITY_CLOSING);
        startService(intent);
        super.onDestroy();
    }

    @Override
    public void onNavigate(IdType type, long id) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, LibraryFragment.createInstance(type, id))
                .addToBackStack(String.valueOf(id)).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
    }

    @Override
    public void onViewModeToggled(IdType type, long id) {
        getSupportFragmentManager().popBackStack();
        onNavigate(type, id);
    }

    @Override
    public void playSongs(Cursor songsCursor, int from) {
        playbackService.play(songsCursor, from);
    }

    @Override
    public void play(IdType type, ArrayList<Long> ids) {
        playbackService.play(type, ids);
    }

    @Override
    public void add(IdType type, ArrayList<Long> ids) {
        playbackService.add(type, ids);
    }

    @Override
    public void addAsNext(IdType type, ArrayList<Long> ids) {
        playbackService.addAsNext(type, ids);
    }

    @Override
    public void onArtistsDbInitialized() {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, LibraryFragment.createRootInstance()).commit();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playbackService = ((PlaybackService.LocalBinder)service).getService();

        nowPlayingBar.setPlaybackService(playbackService);
        nowPlayingBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PlayingNowActivity.class));
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }
}
