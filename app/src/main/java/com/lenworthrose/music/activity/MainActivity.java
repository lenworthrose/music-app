package com.lenworthrose.music.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.lenworthrose.music.R;
import com.lenworthrose.music.fragment.GridFragment;
import com.lenworthrose.music.fragment.ListFragment;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.NavigationListener;

public class MainActivity extends AppCompatActivity implements NavigationListener, ServiceConnection {
    private PlaybackService playbackService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, GridFragment.artistsInstance()).commit();
        startService(new Intent(this, PlaybackService.class));
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
    protected void onStop() {
        super.onStop();
        unbindService(this);
    }

    @Override
    public void onNavigateToArtist(long artistId) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, GridFragment.albumsInstance(artistId)).addToBackStack(String.valueOf(artistId)).commit();
    }

    @Override
    public void onNavigateToAlbum(long albumId) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, ListFragment.songsInstance(albumId)).addToBackStack(String.valueOf(albumId)).commit();
    }

    @Override
    public void playSongs(Cursor songsCursor, int from) {
        playbackService.play(songsCursor, from);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playbackService = ((PlaybackService.LocalBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }
}
