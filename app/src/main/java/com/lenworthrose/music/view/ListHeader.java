package com.lenworthrose.music.view;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.lenworthrose.music.IdType;
import com.lenworthrose.music.R;
import com.lenworthrose.music.activity.LastFmInfoActivity;
import com.lenworthrose.music.adapter.AlbumsAdapter;
import com.lenworthrose.music.sync.ArtistsStore;
import com.lenworthrose.music.util.Utils;

/**
 * A {@link View} that is meant to be added as the header to a list. Contains information about the list (e.g.
 * Artist, Album names) as well as an image, which is shown twice: once in it's unadultered form as a thumbnail,
 * and also as a blurred background for this view.
 */
public class ListHeader extends FrameLayout implements Loader.OnLoadCompleteListener<Cursor> {
    private IdType type;
    private ImageView background, coverArt;
    private TextView artist, album, year, tracksDuration;

    public ListHeader(Context context, IdType type, long id) {
        super(context);
        this.type = type;

        View root = LayoutInflater.from(context).inflate(R.layout.list_header, this);
        background = (ImageView)root.findViewById(R.id.fvh_background);
        coverArt = (ImageView)root.findViewById(R.id.fvh_cover_art);
        artist = (TextView)root.findViewById(R.id.fvh_artist);
        album = (TextView)root.findViewById(R.id.fvh_album);
        year = (TextView)root.findViewById(R.id.fvh_year);
        tracksDuration = (TextView)root.findViewById(R.id.fvh_tracks_duration);
        getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);

        populate(id);
    }

    private void populate(long id) {
        CursorLoader loader;

        switch (type) {
            case ARTIST:
                loader = ArtistsStore.getInstance().getArtistInfo(getContext(), id);
                break;
            case ALBUM:
                loader = AlbumsAdapter.getAlbums(getContext(), IdType.ALBUM, id);
                break;
            default:
                loader = null;
                break;
        }

        if (loader != null) {
            loader.registerListener(0, this);
            loader.startLoading();
        }
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() == 0 || !data.moveToFirst()) {
            setVisibility(View.GONE);
            return;
        }

        switch (type) {
            case ARTIST:
                String bio = data.getString(6) == null ? "" : data.getString(6);
                album.setText(data.getString(2));
                year.setText(Html.fromHtml(bio).toString());
                year.setSingleLine(false);
                year.setMaxLines(5);
                artist.setVisibility(View.GONE);
                tracksDuration.setVisibility(View.GONE);
                setImages(data.getString(4));
                final long artistId = data.getLong(0);

                setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getContext(), LastFmInfoActivity.class);
                        intent.putExtra(LastFmInfoActivity.EXTRA_ID, artistId);
                        getContext().startActivity(intent);
                    }
                });
                break;
            case ALBUM:
                artist.setText(data.getString(5));
                album.setText(data.getString(1));
                tracksDuration.setText(getContext().getResources().getQuantityString(R.plurals.num_of_tracks, data.getInt(2), data.getInt(2)));

                int year = data.getInt(3);
                if (year > 0) this.year.setText(String.valueOf(year));

                setImages(data.getString(4));
                break;
        }

        if (year.getText().length() == 0) year.setVisibility(View.GONE);
        if (album.getText().length() == 0) album.setVisibility(View.GONE);

        data.close();
    }

    private void setImages(String imageUrl) {
        Glide.with(getContext()).load(imageUrl).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                coverArt.setImageBitmap(resource);

                Utils.createBlurredBitmap(resource, new Utils.BitmapCallback() {
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        background.setImageBitmap(bitmap);
                        background.animate().alpha(1f).setDuration(300).start();
                    }
                });
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                coverArt.setVisibility(View.GONE);
                background.setVisibility(View.GONE);
            }
        });
    }

    private ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            background.setLayoutParams(new FrameLayout.LayoutParams(getWidth(), getHeight()));
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    };
}
