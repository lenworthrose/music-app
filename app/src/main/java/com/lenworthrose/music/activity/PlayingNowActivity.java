package com.lenworthrose.music.activity;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.app.NavUtils;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;

import com.astuetz.PagerSlidingTabStrip;
import com.lenworthrose.music.R;
import com.lenworthrose.music.fragment.PlayingItemFragment;
import com.lenworthrose.music.fragment.PlayingNowPlaylistFragment;
import com.lenworthrose.music.fragment.ShuffleDialogFragment;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.widget.WidgetService;

/**
 * The Playing Now page for the application. Displays the {@link PlayingItemFragment} and {@link PlayingNowPlaylistFragment}.
 * <p/>
 * Contains an ImageView for displaying a shared background, which is used when displaying the two Fragments in a ViewPager.
 */
public class PlayingNowActivity extends AppCompatActivity {
    private ImageView background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.SETTING_KEEP_SCREEN_ON, false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_playing_now);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) postponeEnterTransition();

        background = (ImageView)findViewById(R.id.pn_background_image);
        ViewPager pager = (ViewPager)findViewById(R.id.pn_root_container);

        if (pager != null) {
            pager.setAdapter(new PlayingNowTabPagerAdapter(getSupportFragmentManager()));
            ((PagerSlidingTabStrip)findViewById(R.id.pn_root_tabs)).setViewPager(pager);
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.pn_placeholder, new PlayingItemFragment())
                    .replace(R.id.pnp_placeholder, new PlayingNowPlaylistFragment())
                    .commit();
        }

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = new Intent(this, WidgetService.class);
        intent.setAction(WidgetService.ACTION_REFRESH);
        this.startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_playing_now, menu);
        return true;
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

    public void setBackgroundImage(Bitmap bitmap) {
        Bitmap old = background.getDrawable() instanceof BitmapDrawable ? ((BitmapDrawable)background.getDrawable()).getBitmap() : null;

        if (bitmap != null)
            background.setImageBitmap(bitmap);
        else
            background.setImageResource(android.R.color.transparent);

        if (old != null) old.recycle();
    }

    public void onShuffleClicked(MenuItem unused) {
        new ShuffleDialogFragment().show(getFragmentManager(), "Shuffle");
    }

    public void onEditPlaylistClicked(MenuItem unused) {
        startActivity(new Intent(this, EditPlaylistActivity.class));
    }

    private class PlayingNowTabPagerAdapter extends FragmentPagerAdapter {
        public PlayingNowTabPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int index) {
            switch (index) {
                case 0:
                    return new PlayingItemFragment();
                case 1:
                    return new PlayingNowPlaylistFragment();
            }

            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.playing_now);
                case 1:
                    return getString(R.string.playlist);
            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public void onEqualizerClicked(MenuItem unused) {
        startActivity(new Intent(this, EqualizerActivity.class));
    }
}
