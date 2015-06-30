package com.lenworthrose.music.activity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ImageView;

import com.astuetz.PagerSlidingTabStrip;
import com.lenworthrose.music.R;
import com.lenworthrose.music.fragment.PlayingItemFragment;
import com.lenworthrose.music.fragment.PlayingNowPlaylistFragment;
import com.lenworthrose.music.util.Constants;

/**
 * The Playing Now page for the application. Displays the {@link PlayingItemFragment} and {@link PlayingNowPlaylistFragment}.
 *
 * Contains an ImageView for displaying a shared background, which is used when displaying the two Fragments in a ViewPager.
 */
public class PlayingNowActivity extends AppCompatActivity {
    private ImageView background;
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.SETTING_KEEP_SCREEN_ON, false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.act_playing_now);

        background = (ImageView)findViewById(R.id.pn_background_image);
        pager = (ViewPager)findViewById(R.id.pn_root_container);

        if (pager != null) {
            pager.setAdapter(new PlayingNowTabPagerAdapter(getSupportFragmentManager()));
            ((PagerSlidingTabStrip)findViewById(R.id.pn_root_tabs)).setViewPager(pager);
        }
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

    public void setBackgroundImage(Bitmap bitmap) {
        Bitmap old = background.getDrawable() instanceof BitmapDrawable ? ((BitmapDrawable)background.getDrawable()).getBitmap() : null;

        if (bitmap != null)
            background.setImageBitmap(bitmap);
        else
            background.setImageResource(android.R.color.transparent);

        if (old != null) old.recycle();
    }
}
