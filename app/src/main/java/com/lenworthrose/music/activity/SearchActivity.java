package com.lenworthrose.music.activity;

import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.app.NavUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.fragment.LibraryFragment;
import com.lenworthrose.music.util.Constants;

/**
 * Activity that shows the results when long-pressing on one of {@link com.lenworthrose.music.fragment.PlayingItemFragment}'s fields.
 */
public class SearchActivity extends NavigationActivity {
    private BroadcastReceiver modificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getExtras().getInt(Constants.EXTRA_MODIFICATION_TYPE)) {
                case Constants.EXTRA_MODIFICATION_TYPE_PLAY:
                    finish();
                    break;
                case Constants.EXTRA_MODIFICATION_TYPE_ADD:
                    Toast.makeText(SearchActivity.this, R.string.added, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.EXTRA_MODIFICATION_TYPE_ADD_AS_NEXT:
                    Toast.makeText(SearchActivity.this, R.string.added_as_next, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent data = getIntent();
        setTitle(data.getStringExtra(Constants.EXTRA_TITLE));
        LibraryFragment fragment;

        if (data.hasExtra(Constants.EXTRA_ARTIST_ID))
            fragment = LibraryFragment.createInstance(IdType.ARTIST, data.getLongExtra(Constants.EXTRA_ARTIST_ID, -1));
        else
            fragment = LibraryFragment.createInstance(IdType.ALBUM, data.getLongExtra(Constants.EXTRA_ALBUM_ID, -1));

        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(modificationReceiver, new IntentFilter(Constants.PLAYBACK_MODIFICATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(modificationReceiver);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);

                if (NavUtils.shouldUpRecreateTask(this, upIntent))
                    TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
                else
                    NavUtils.navigateUpTo(this, upIntent);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
