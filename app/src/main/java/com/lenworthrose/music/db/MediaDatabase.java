package com.lenworthrose.music.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * Created by Lenny on 2015-06-25.
 */
public class MediaDatabase {
    private ContentResolver resolver;

    public MediaDatabase(Context context) {
        resolver = context.getContentResolver();
    }

    public Bitmap getArtistArt(long artistId) {
        return null;
    }

    public Bitmap getAlbumArt(long albumId) {
        Bitmap bm = null;

        try {
            final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");

            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
        } catch (Exception ex) {
            Log.w("MediaDatabase", ex.getClass().getName() + " occurred attempting to get album art for id=" + albumId, ex);
        }

        return bm;
    }
}
