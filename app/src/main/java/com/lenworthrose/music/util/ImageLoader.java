package com.lenworthrose.music.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public class ImageLoader {
    private LruBitmapCache thumbnailCache;

    private static class LazyHolder {
        private static final ImageLoader INSTANCE = new ImageLoader();
    }

    private ImageLoader() {
        thumbnailCache = new LruBitmapCache(20 * 1024 * 1024);
    }

    public static ImageLoader getInstance() {
        return LazyHolder.INSTANCE;
    }

    public interface ImageLoadListener {
        void onStartingLoad(AsyncTask<?, ?, ?> task);
        void onImageLoaded(Bitmap image);
    }

    public void loadImage(final Context context, String uri, ImageLoadListener listener) {
        final WeakReference<ImageLoadListener> callback = new WeakReference<>(listener);

        AsyncTask<String, Void, Bitmap> task = new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                String artPath = params[0];
                if (isCancelled()) return null;
                return Utils.getBitmapForContentUri(context, artPath);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (callback.get() != null) callback.get().onImageLoaded(bitmap);
            }
        };

        listener.onStartingLoad(task);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
    }

    public void loadImage(String uri, ImageLoadListener listener) {
        final WeakReference<ImageLoadListener> callback = new WeakReference<>(listener);

        AsyncTask<String, Void, Bitmap> task = new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                String artPath = params[0];
                if (isCancelled()) return null;
                return BitmapFactory.decodeFile(artPath);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (callback.get() != null) callback.get().onImageLoaded(bitmap);
            }
        };

        listener.onStartingLoad(task);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
    }

    public void loadThumbnail(final Context context, String uri, ImageLoadListener listener) {
        final WeakReference<ImageLoadListener> callback = new WeakReference<>(listener);

        AsyncTask<String, Void, Bitmap> task = new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                String artPath = params[0];
                if (isCancelled()) return null;
                Bitmap retVal = thumbnailCache.getBitmap(artPath);
                if (retVal != null) return retVal;

                if (isCancelled()) return null;
                Bitmap art = Utils.getBitmapForContentUri(context, artPath);

                if (art != null) {
                    Bitmap scaledArt = Bitmap.createScaledBitmap(art, 200, 200, false);
                    art.recycle();
                    art = scaledArt;
                    thumbnailCache.putBitmap(artPath, art);
                }

                return art;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (callback.get() != null) callback.get().onImageLoaded(bitmap);
            }
        };

        listener.onStartingLoad(task);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
    }
}
