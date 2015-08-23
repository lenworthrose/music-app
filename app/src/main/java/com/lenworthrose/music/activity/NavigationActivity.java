package com.lenworthrose.music.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.fragment.LibraryFragment;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.NavigationListener;

import java.util.ArrayList;

/**
 * An Activity that implements {@link AppCompatActivity} in order to handle navigation events.
 *
 * Classes that extend this class MUST include a {@link android.view.ViewGroup} with the id root_container, otherwise
 * attempting to navigate will cause an Exception!
 */
public class NavigationActivity extends AppCompatActivity implements NavigationListener, ServiceConnection {
    protected PlaybackService playbackService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, PlaybackService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(this);
        playbackService = null;
        super.onStop();
    }

    @Override
    public void onNavigate(IdType type, long id) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, LibraryFragment.createInstance(type, id))
                .addToBackStack(String.valueOf(id)).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
    }

    @Override
    public void onViewModeToggled(IdType type, long id) {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            getSupportFragmentManager().beginTransaction().replace(R.id.root_container, LibraryFragment.createInstance(type, id)).commit();
        } else {
            getSupportFragmentManager().popBackStack();
            getSupportFragmentManager().beginTransaction().replace(R.id.root_container, LibraryFragment.createInstance(type, id))
                    .addToBackStack(String.valueOf(id)).commit();
        }
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
    public void onServiceConnected(ComponentName name, IBinder service) {
        playbackService = ((PlaybackService.LocalBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }
}
