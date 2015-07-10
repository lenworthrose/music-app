package com.lenworthrose.music.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lenworthrose.music.R;
import com.lenworthrose.music.sync.ArtistsStore;

/**
 * Activity to show information retrieved from Last.fm.
 */
public class LastFmInfoActivity extends AppCompatActivity implements Loader.OnLoadCompleteListener<Cursor> {
    public static final String EXTRA_ID = "ID";

    private ImageView image;
    private TextView name, bio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_last_fm_info);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) savedInstanceState = getIntent().getExtras();
        long id = savedInstanceState.getLong(EXTRA_ID);

        CursorLoader cursorLoader = ArtistsStore.getInstance().getArtistInfo(this, id);
        cursorLoader.registerListener(0, this);
        cursorLoader.startLoading();

        image = (ImageView)findViewById(R.id.lfi_artist_image);
        name = (TextView)findViewById(R.id.lfi_artist_name);
        bio = (TextView)findViewById(R.id.lfi_artist_bio);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpTo(this, NavUtils.getParentActivityIntent(this));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, final Cursor data) {
        if (data.moveToFirst()) {
            name.setText(data.getString(2));
            bio.setText(Html.fromHtml(data.getString(6)));
            Glide.with(this).load(data.getString(4)).into(image);
        }
    }
}
