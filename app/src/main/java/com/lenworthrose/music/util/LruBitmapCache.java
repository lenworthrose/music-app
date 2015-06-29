package com.lenworthrose.music.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;

public class LruBitmapCache extends LruCache<String, Bitmap> {
    protected LruBitmapCache(int maxSize) {
        super(maxSize);
    }

    public LruBitmapCache(Context ctx) {
        this(getCacheSize(ctx));
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    public Bitmap getBitmap(String albumId) {
        return get(albumId);
    }

    public void putBitmap(String albumId, Bitmap bitmap) {
        put(albumId, bitmap);
    }

    // Returns a cache size equal to approximately two screens worth of images.
    private static int getCacheSize(Context ctx) {
        final DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
        final int screenWidth = displayMetrics.widthPixels;
        final int screenHeight = displayMetrics.heightPixels;
        final int screenBytes = screenWidth * screenHeight * 4; //4 bytes per pixel (ARGB)

        return screenBytes * 2;
    }
}
