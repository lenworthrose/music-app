package com.lenworthrose.music;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.lenworthrose.music.fragment.GridFragment;
import com.lenworthrose.music.fragment.ListFragment;
import com.lenworthrose.music.helper.NavigationListener;

public class MainActivity extends AppCompatActivity implements NavigationListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, GridFragment.artistsInstance()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onNavigateToArtist(long artistId) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, GridFragment.albumsInstance(artistId)).addToBackStack(String.valueOf(artistId)).commit();
    }

    @Override
    public void onNavigateToAlbum(long albumId) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, ListFragment.songsInstance(albumId)).addToBackStack(String.valueOf(albumId)).commit();
    }
}
