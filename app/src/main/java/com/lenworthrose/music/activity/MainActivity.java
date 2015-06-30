package com.lenworthrose.music.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.SongsAdapter;
import com.lenworthrose.music.fragment.NavigationFragment;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.NavigationListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationListener, ServiceConnection {
    private PlaybackService playbackService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, NavigationFragment.createRootInstance()).commit();
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
    public void onNavigate(IdType type, long id) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, NavigationFragment.createInstance(type, id))
                .addToBackStack(String.valueOf(id)).commit();
    }

    @Override
    public void playSongs(Cursor songsCursor, int from) {
        playbackService.play(songsCursor, from);
    }

    @Override
    public void play(IdType type, ArrayList<Long> ids) {
        Bundle b = new Bundle();
        b.putSerializable(PlaybackLoaderCallbacks.IDS, ids);
        b.putSerializable(PlaybackLoaderCallbacks.TYPE, type);
        getSupportLoaderManager().restartLoader(0, b, new PlayLoaderCallbacks());
    }

    @Override
    public void add(IdType type, ArrayList<Long> ids) {
        Bundle b = new Bundle();
        b.putSerializable(PlaybackLoaderCallbacks.IDS, ids);
        b.putSerializable(PlaybackLoaderCallbacks.TYPE, type);
        getSupportLoaderManager().restartLoader(0, b, new AddLoaderCallbacks());
    }

    @Override
    public void addAsNext(IdType type, ArrayList<Long> ids) {
        Bundle b = new Bundle();
        b.putSerializable(PlaybackLoaderCallbacks.IDS, ids);
        b.putSerializable(PlaybackLoaderCallbacks.TYPE, type);
        getSupportLoaderManager().restartLoader(0, b, new AddAsNextLoaderCallbacks());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playbackService = ((PlaybackService.LocalBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }

    private abstract class PlaybackLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        public static final String TYPE = "Type";
        public static final String IDS = "IDs";

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return SongsAdapter.createSongsLoader(MainActivity.this, (IdType)args.get(TYPE), (ArrayList<Long>)args.getSerializable(IDS));
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) { }
    }

    private class PlayLoaderCallbacks extends PlaybackLoaderCallbacks {
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            playbackService.play(data, 0);
        }
    }

    private class AddLoaderCallbacks extends PlaybackLoaderCallbacks {
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            playbackService.add(data);
        }
    }

    private class AddAsNextLoaderCallbacks extends PlaybackLoaderCallbacks {
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            playbackService.addAsNext(data);
        }
    }
}
