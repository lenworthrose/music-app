package com.lenworthrose.music.activity;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.NavigationDrawerAdapter;
import com.lenworthrose.music.fragment.LibraryFragment;
import com.lenworthrose.music.fragment.SearchFragment;
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
public class MainActivity extends AppCompatActivity implements NavigationListener, ServiceConnection, ArtistsStore.InitListener, AdapterView.OnItemClickListener {
    private PlaybackService playbackService;
    private NowPlayingBar nowPlayingBar;
    private BroadcastReceiver receiver;
    private ListView drawerListView;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private int selectedDrawerPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        nowPlayingBar = (NowPlayingBar)findViewById(R.id.main_now_playing_bar);
        nowPlayingBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayingNow();
            }
        });

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout)findViewById(R.id.main_drawer_layout);
        drawerListView = (ListView)findViewById(R.id.main_drawer_list_view);
        drawerListView.setAdapter(new NavigationDrawerAdapter(this));
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        drawerListView.setOnItemClickListener(this);

        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(Constants.CMD_ACTIVITY_STARTING);
        startService(intent);

        ArtistsStore.getInstance().init(this, this);

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

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("FIRST_LAUNCH", true))
            showWelcomeDialog();
        else
            startService(new Intent(MainActivity.this, MediaStoreService.class));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) return true;

        switch (item.getItemId()) {
            case R.id.action_playing_now:
                showPlayingNow();
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
        nowPlayingBar.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        nowPlayingBar.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        unbindService(this);
        playbackService = null;
        nowPlayingBar.setPlaybackService(null);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(Constants.CMD_ACTIVITY_CLOSING);
        startService(intent);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
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
        if (getSupportFragmentManager().getFragments() != null && getSupportFragmentManager().getFragments().size() > 0)
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selectedDrawerPosition = Integer.parseInt(prefs.getString(Constants.SETTING_START_LOCATION, "0"));
        onItemClick(drawerListView, null, selectedDrawerPosition, selectedDrawerPosition);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playbackService = ((PlaybackService.LocalBinder)service).getService();
        nowPlayingBar.setPlaybackService(playbackService);
        nowPlayingBar.onResume();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
        nowPlayingBar.setPlaybackService(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        IdType type = null;

        switch ((int)id) {
            case 0:
                type = IdType.ARTIST;
                break;
            case 1:
                type = IdType.ALBUM;
                break;
            case 2:
                type = IdType.SONG;
                break;
            case 3:
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction().replace(R.id.root_container, new SearchFragment()).commit();
                drawerLayout.closeDrawers();
                return;
            case 4:
                postCloseDrawer();
                startActivity(new Intent(this, PlayingNowActivity.class));
                return;
            case 5:
                postCloseDrawer();
                startActivity(new Intent(this, SettingsActivity.class));
                return;
        }

        selectedDrawerPosition = position;
        drawerListView.setItemChecked(selectedDrawerPosition, true);

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, LibraryFragment.createRootInstance(type)).commit();
        drawerLayout.closeDrawers();
    }

    private void postCloseDrawer() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                drawerListView.setItemChecked(selectedDrawerPosition, true);
                drawerLayout.closeDrawers();
            }
        }, 1000);
    }

    private void showWelcomeDialog() {
        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean lastFmEnabled = which == DialogInterface.BUTTON_POSITIVE;
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                editor.putBoolean("FIRST_LAUNCH", false).putBoolean(Constants.SETTING_LAST_FM_INTEGRATION, lastFmEnabled).apply();
                startService(new Intent(MainActivity.this, MediaStoreService.class));
            }
        };

        new AlertDialog.Builder(this).setTitle(R.string.welcome_dialog_title).setMessage(R.string.welcome_dialog_message)
                .setPositiveButton(android.R.string.yes, buttonListener).setNegativeButton(android.R.string.no, buttonListener)
                .show();
    }

    private void showPlayingNow() {
        Bundle options = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            options = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this, nowPlayingBar.findViewById(R.id.np_cover), "now_playing").toBundle();

        startActivity(new Intent(MainActivity.this, PlayingNowActivity.class), options);
    }
}
