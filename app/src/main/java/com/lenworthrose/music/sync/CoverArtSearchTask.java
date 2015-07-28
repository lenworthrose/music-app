package com.lenworthrose.music.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

/**
 * {@link AsyncTask} that will search for cover art in the folder that contains the album.
 */
class CoverArtSearchTask extends AsyncTask<Void, Integer, Void> {
    private Context context;
    private SQLiteDatabase db;

    public CoverArtSearchTask(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Cursor albumsMissingArt = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Albums._ID, MediaStore.Audio.Media.ARTIST_ID },
                MediaStore.Audio.Albums.ALBUM_ART + " IS NULL",
                null,
                null);

        if (albumsMissingArt.moveToFirst()) {
            int current = 0, total = albumsMissingArt.getCount();
            long artistId = albumsMissingArt.getLong(1);

            do {
                publishProgress(current++, total);
                long albumId = albumsMissingArt.getLong(0);
                File albumDir = getAlbumDirectory(context, albumId);
                String artPath = getLargestImagePath(albumDir);

                if (artPath != null) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Albums.ALBUM_ID, albumId);
                    values.put(MediaStore.Audio.Media.DATA, artPath);
                    context.getContentResolver().insert(Uri.parse(Constants.EXTERNAL_ALBUM_ART_URL), values);

                    if (artistId != albumsMissingArt.getLong(1))
                        updateArtistAlbumArt(artistId, Utils.getAlbumArtUrls(context, artistId));
                }
            } while (albumsMissingArt.moveToNext());

            updateArtistAlbumArt(artistId, Utils.getAlbumArtUrls(context, artistId));
        }

        albumsMissingArt.close();
        return null;
    }

    private static File getAlbumDirectory(Context context, long albumId) {
        Cursor songs = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Media.DATA },
                MediaStore.Audio.Media.ALBUM_ID + "=?",
                new String[] { String.valueOf(albumId) },
                MediaStore.Audio.Media.TRACK);

        if (!songs.moveToFirst()) return null;

        File file = new File(songs.getString(0));

        if (!file.exists()) {
            Log.w("CoverArtSearchTask", "Path for song in MediaStore does not exist?? path=" + songs.getString(0));
            return null;
        }

        songs.close();
        String parentStr = file.getParent();
        if (TextUtils.isEmpty(parentStr)) return null;

        File parentDir = new File(parentStr);
        return parentDir.exists() ? parentDir : null;
    }

    private static String getLargestImagePath(File albumDir) {
        if (albumDir == null) return null;

        File[] images = albumDir.listFiles(new ImageFilenameFilter());
        if (images == null || images.length == 0) return null;

        File biggestArt = images[0];

        for (int i = 1; i < images.length; i++)
            if (biggestArt.length() < images[i].length())
                biggestArt = images[i];

        return biggestArt.getAbsolutePath();
    }

    private void updateArtistAlbumArt(long id, String... albumArtUris) {
        ContentValues values = new ContentValues();

        if (albumArtUris != null) {
            values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_1, albumArtUris[0]);
            values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_2, albumArtUris[1]);
            values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_3, albumArtUris[2]);
            values.put(ArtistsStoreContract.ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_4, albumArtUris[3]);
        }

        db.update(ArtistsStore.TABLE_NAME, values, ArtistsStoreContract.ArtistEntry._ID + "=?", new String[] { String.valueOf(id) });
    }

    private static final class ImageFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String filename) {
            filename = filename.toLowerCase(Locale.ENGLISH);
            return filename.endsWith("jpg") || filename.endsWith("jpeg");
        }
    }
}
