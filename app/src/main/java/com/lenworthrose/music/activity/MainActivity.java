package com.lenworthrose.music.activity;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
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
import com.lenworthrose.music.sync.MediaStoreSyncService;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.view.NowPlayingBar;

/**
 * The main Activity for the application. Extends {@link NavigationActivity} so it can handle navigation
 * events. Manages {@link LibraryFragment}s that display the media library.
 * <p/>
 * Binds to the {@link PlaybackService} so it can handle actions that modify the Playing Now playlist.
 * <p/>
 * Responsible for starting the {@link MediaStoreSyncService}.
 */
public class MainActivity extends NavigationActivity implements AdapterView.OnItemClickListener {
    private NowPlayingBar nowPlayingBar;
    private ListView drawerListView;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private int selectedDrawerPosition;

    private BroadcastReceiver modificationReceiver = new BroadcastReceiver() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        if (savedInstanceState == null) {
            BroadcastReceiver artistsStoreInitReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(this);

                    if (!isFinishing() && !isDestroyed()) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        selectedDrawerPosition = Integer.parseInt(prefs.getString(Constants.SETTING_START_LOCATION, "0"));
                        drawerListView.setItemChecked(selectedDrawerPosition, true);
                        drawerListView.post(new Runnable() {
                            @Override
                            public void run() {
                                onItemClick(drawerListView, null, selectedDrawerPosition, selectedDrawerPosition);
                            }
                        });
                    }
                }
            };

            LocalBroadcastManager.getInstance(this).registerReceiver(artistsStoreInitReceiver, new IntentFilter(Constants.ARTISTS_STORE_INITIALIZED));
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("FIRST_LAUNCH", true))
            showWelcomeDialog();
        else
            startService(new Intent(MainActivity.this, MediaStoreSyncService.class));
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
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(modificationReceiver, new IntentFilter(Constants.PLAYBACK_MODIFICATION_COMPLETE));
        nowPlayingBar.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(modificationReceiver);
        nowPlayingBar.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        nowPlayingBar.setPlaybackService(null);
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void setPlaybackService(PlaybackService service) {
        super.setPlaybackService(service);
        nowPlayingBar.setPlaybackService(service);
        if (service != null) nowPlayingBar.onResume();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Fragment toShow = null;

        switch ((int)id) {
            case 0:
                toShow = LibraryFragment.createRootInstance(IdType.ARTIST);
                break;
            case 1:
                toShow = LibraryFragment.createRootInstance(IdType.ALBUM);
                break;
            case 2:
                toShow = LibraryFragment.createRootInstance(IdType.SONG);
                break;
            case 3:
                toShow = new SearchFragment();
                break;
            case 4:
                postCloseDrawer();
                showPlayingNow();
                return;
            case 5:
                postCloseDrawer();
                startActivity(new Intent(this, SettingsActivity.class));
                return;
        }

        selectedDrawerPosition = position;
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, toShow).commit();
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
                startService(new Intent(MainActivity.this, MediaStoreSyncService.class));
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

        startActivityForResult(new Intent(MainActivity.this, PlayingNowActivity.class), 666, options);
    }
}
