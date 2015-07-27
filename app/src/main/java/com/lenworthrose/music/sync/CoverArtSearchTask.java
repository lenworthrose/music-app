package com.lenworthrose.music.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.lenworthrose.music.util.Utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

/**
 * {@link AsyncTask} that will search for cover art in the folder that contains the album.
 */
public class CoverArtSearchTask extends AsyncTask<Context, Integer, Void> {
    @Override
    protected Void doInBackground(Context... params) {
        Context context = params[0];

        Cursor albumsMissingArt = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Albums._ID },
                MediaStore.Audio.Albums.ALBUM_ART + " IS NULL",
                null,
                null);

        if (albumsMissingArt.moveToFirst()) {
            int current = 0, total = albumsMissingArt.getCount();

            do {
                publishProgress(current++, total);
                long albumId = albumsMissingArt.getLong(0);

                Cursor songs = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Media.DATA },
                        MediaStore.Audio.Media.ALBUM_ID + "=?",
                        new String[] { String.valueOf(albumId) },
                        MediaStore.Audio.Media.TRACK);

                if (!songs.moveToFirst()) continue;

                File file = new File(songs.getString(0));

                if (!file.exists()) {
                    Log.w("CoverArtSearchTask", "Path for song in MediaStore does not exist?? path=" + songs.getString(0));
                    continue;
                }

                songs.close();
                String parentStr = file.getParent();
                if (TextUtils.isEmpty(parentStr)) continue;

                File parentDir = new File(parentStr);
                if (!parentDir.exists()) continue;

                File[] images = parentDir.listFiles(new ImageFilenameFilter());

                if (images != null && images.length > 0) {
                    File biggestArt = images[0];

                    for (int i = 1; i < images.length; i++)
                        if (biggestArt.length() < images[i].length())
                            biggestArt = images[i];

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Albums.ALBUM_ID, albumId);
                    values.put(MediaStore.Audio.Media.DATA, biggestArt.getAbsolutePath());

                    context.getContentResolver().insert(Uri.parse(Utils.buildAlbumArtUrl(albumId)), values);
                }
            } while (albumsMissingArt.moveToNext());
        }

        albumsMissingArt.close();

        return null;
    }

    private static final class ImageFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String filename) {
            filename = filename.toLowerCase(Locale.ENGLISH);
            return filename.endsWith("jpg") || filename.endsWith("jpeg");
        }
    }
}
