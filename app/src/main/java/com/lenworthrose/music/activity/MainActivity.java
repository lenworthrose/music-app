package com.lenworthrose.music.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.SongsAdapter;
import com.lenworthrose.music.fragment.LibraryFragment;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.sync.ArtistsStore;
import com.lenworthrose.music.sync.MediaStoreService;
import com.lenworthrose.music.util.NavigationListener;

import java.util.ArrayList;

/**
 * The main Activity for the application. Implements {@link NavigationListener} so it can handle navigation
 * events. Manages the {@link LibraryFragment}s that display the media library.
 *
 * Responsible for starting the {@link PlaybackService}. Also binds to the Service so it can modify the Playing Now playlist.
 *
 * Responsible for starting the {@link MediaStoreService}.
 */
public class MainActivity extends AppCompatActivity implements NavigationListener, ServiceConnection, ArtistsStore.InitListener {
    private PlaybackService playbackService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, PlaybackService.class));
        ArtistsStore.getInstance().init(this, this);
        startService(new Intent(this, MediaStoreService.class));
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
    protected void onDestroy() {
        stopService(new Intent(this, PlaybackService.class));
        super.onDestroy();
    }

    @Override
    public void onNavigate(IdType type, long id) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, LibraryFragment.createInstance(type, id))
                .addToBackStack(String.valueOf(id)).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
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

    @Override
    public void onArtistsDbInitialized() {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, LibraryFragment.createRootInstance()).commit();
    }

    private abstract class PlaybackLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        public static final String TYPE = "Type";
        public static final String IDS = "IDs";

        protected IdType type;
        protected ArrayList<Long> ids;

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            type = (IdType)args.get(TYPE);
            ids = (ArrayList<Long>)args.getSerializable(IDS);
            ArrayList<Long> nextId = new ArrayList<>(1);
            nextId.add(ids.remove(0));
            return SongsAdapter.createSongsLoader(MainActivity.this, type, nextId);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) { }
    }

    private class PlayLoaderCallbacks extends PlaybackLoaderCallbacks {
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            playbackService.play(data, 0);
            if (!ids.isEmpty()) add(type, ids);
        }
    }

    private class AddLoaderCallbacks extends PlaybackLoaderCallbacks {
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            playbackService.add(data);
            if (!ids.isEmpty()) add(type, ids);
        }
    }

    private class AddAsNextLoaderCallbacks extends PlaybackLoaderCallbacks {
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            playbackService.addAsNext(data);
            if (!ids.isEmpty()) addAsNext(type, ids);
        }
    }
}
