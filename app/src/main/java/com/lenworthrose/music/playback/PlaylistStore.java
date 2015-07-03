package com.lenworthrose.music.playback;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.lenworthrose.music.adapter.PlayingNowPlaylistAdapter;
import com.lenworthrose.music.playback.PlaylistStoreContract.PlaylistEntry;
import com.mobeta.android.dslv.DragSortCursorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages the Playing Now Playlist database. Contains methods to add tracks from
 * a {@link Cursor} containing Song data from the {@link android.provider.MediaStore}.
 */
public class PlaylistStore {
    public static final String TABLE_NAME = "playlist";

    public interface InitListener {
        void onInitComplete();
    }

    private SQLiteDatabase db;
    private InitTask initTask;
    private InitListener initListener;

    public void init(Context context) {
        initTask = new InitTask(context);
        initTask.execute();
    }

    public void setListener(InitListener listener) {
        this.initListener = listener;
    }

    public Cursor read() {
        if (db == null) {
            Log.e("PlaylistStore", "db was null when attempting read!");
            return null;
        }

        String[] projection = {
                PlaylistEntry._ID,
                PlaylistEntry.COLUMN_SEQUENCE,
                PlaylistEntry.COLUMN_SONG_ID,
                PlaylistEntry.COLUMN_ARTIST,
                PlaylistEntry.COLUMN_ALBUM,
                PlaylistEntry.COLUMN_NAME,
                PlaylistEntry.COLUMN_TRACK_NUM,
                PlaylistEntry.COLUMN_DURATION,
                PlaylistEntry.COLUMN_ALBUM_ART_URL
        };

        return db.query(TABLE_NAME, projection, null, null, null, null, PlaylistEntry.COLUMN_SEQUENCE + " ASC");
    }

    public void setPlaylist(Cursor songsCursor) {
        if (db == null) {
            Log.e("PlaylistStore", "db was null when attempting write!");
            return;
        }

        db.beginTransaction();

        try {
            db.delete(TABLE_NAME, null, null);

            if (songsCursor != null && songsCursor.moveToFirst()) {
                int sequence = 0;

                do {
                    db.insert(TABLE_NAME, null, createContentValuesFromSongsCursor(songsCursor, sequence++));
                } while (songsCursor.moveToNext());
            }

            db.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e("PlaylistStore", "Exception occurred writing to playlist SQL database.", ex);
        } finally {
            db.endTransaction();
        }
    }

    public void clear() {
        setPlaylist(null);
    }

    public void add(Cursor songsCursor) {
        if (!songsCursor.moveToFirst()) return;

        Cursor cur = read();
        int seq = cur.getCount();
        cur.close();
        db.beginTransaction();

        try {
            do {
                db.insert(TABLE_NAME, null, createContentValuesFromSongsCursor(songsCursor, seq++));
            } while (songsCursor.moveToNext());

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void addAfter(int position, Cursor songsCursor) {
        if (!songsCursor.moveToFirst()) return;

        Cursor cur = read();
        cur.moveToFirst();
        db.beginTransaction();

        try {
            String tempTableName = "playlist_temp";
            db.execSQL(PlaylistStoreContract.buildCreatePlaylistTableSql(tempTableName, true));
            db.delete(tempTableName, null, null);
            int sequence = 0;

            while (!cur.isAfterLast() && cur.getInt(1) < position) {
                db.insert(tempTableName, null, createContentValuesFromPlaylistCursor(cur, sequence++));
                cur.moveToNext();
            }

            do {
                db.insert(tempTableName, null, createContentValuesFromSongsCursor(songsCursor, sequence++));
            } while (songsCursor.moveToNext());

            while (!cur.isAfterLast()) {
                db.insert(tempTableName, null, createContentValuesFromPlaylistCursor(cur, sequence++));
                cur.moveToNext();
            }

            db.delete(TABLE_NAME, null, null);
            db.execSQL(buildMigrateDataFromTempSql(tempTableName));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            cur.close();
        }
    }

    public void shuffle(int from) {
        db.beginTransaction();

        try {
            String[] projection = { PlaylistEntry._ID };

            Cursor cur = db.query(TABLE_NAME, projection, PlaylistEntry.COLUMN_SEQUENCE + " > " + String.valueOf(from), null, null, null, PlaylistEntry.COLUMN_SEQUENCE + " ASC");
            List<Long> toUpdate = new ArrayList<>();
            List<Long> newOrder = new ArrayList<>();

            if (cur.moveToFirst())
                while (!cur.isAfterLast()) {
                    toUpdate.add(cur.getLong(0));
                    cur.moveToNext();
                }

            cur.close();

            Random rand = new Random();

            while (!toUpdate.isEmpty())
                newOrder.add(toUpdate.remove(rand.nextInt(toUpdate.size())));

            for (long id : newOrder) {
                ContentValues values = new ContentValues();
                values.put(PlaylistEntry.COLUMN_SEQUENCE, ++from);

                db.update(TABLE_NAME, values, PlaylistEntry._ID + " = " + String.valueOf(id), null);
            }

            db.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e("PlaylistStore", "Exception occurred shuffling playlist SQL database.", ex);
        } finally {
            db.endTransaction();
        }
    }

    public void performPlaylistActions(PlayingNowPlaylistAdapter adapter) {
        Cursor cur = adapter.getCursor();
        cur.moveToPosition(-1);

        db.beginTransaction();

        try {
            while (cur.moveToNext()) {
                int listPos = adapter.getListPosition(cur.getPosition());

                if (listPos == DragSortCursorAdapter.REMOVED) {
                    Log.d("PlaylistStore", "Playlist item removed: cursorPosition=" + cur.getPosition() + " name=" + cur.getString(6) + " _id=" + cur.getLong(0));
                    db.delete(TABLE_NAME, PlaylistEntry._ID + " = " + String.valueOf(cur.getLong(0)), null);
                } else if (listPos != cur.getPosition()) {
                    ContentValues values = new ContentValues();
                    values.put(PlaylistEntry.COLUMN_SEQUENCE, listPos);

                    db.update(TABLE_NAME, values, PlaylistEntry._ID + " = " + String.valueOf(cur.getLong(0)), null);
                    Log.d("PlaylistStore", "Playlist item moved: cursorPosition=" + cur.getPosition() + " listPos=" + listPos + " name=" + cur.getString(6) + " _id=" + cur.getLong(0));
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int findPosition(long songId) {
        int retVal = -1;
        Cursor cur = db.query(TABLE_NAME, new String[] { PlaylistEntry.COLUMN_SEQUENCE }, PlaylistEntry.COLUMN_SONG_ID + " = " + songId, null, null, null, null);
        if (cur.moveToFirst()) retVal = cur.getInt(0);
        cur.close();

        return retVal;
    }

    private ContentValues createContentValuesFromSongsCursor(Cursor cur, int sequence) {
        ContentValues values = new ContentValues();
        values.put(PlaylistEntry.COLUMN_SEQUENCE, sequence);
        values.put(PlaylistEntry.COLUMN_SONG_ID, cur.getLong(0));
        values.put(PlaylistEntry.COLUMN_NAME, cur.getString(1));
        values.put(PlaylistEntry.COLUMN_TRACK_NUM, cur.getInt(2));
        values.put(PlaylistEntry.COLUMN_DURATION, cur.getLong(3));
        values.put(PlaylistEntry.COLUMN_ARTIST, cur.getString(4));
        values.put(PlaylistEntry.COLUMN_ALBUM, cur.getString(5));

        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        albumArtUri = ContentUris.withAppendedId(albumArtUri, cur.getLong(6));
        values.put(PlaylistEntry.COLUMN_ALBUM_ART_URL, albumArtUri.toString());

        return values;
    }

    private ContentValues createContentValuesFromPlaylistCursor(Cursor cur, int sequence) {
        ContentValues values = new ContentValues();
        values.put(PlaylistEntry.COLUMN_SEQUENCE, sequence);
        values.put(PlaylistEntry.COLUMN_SONG_ID, cur.getLong(2));
        values.put(PlaylistEntry.COLUMN_ARTIST, cur.getString(3));
        values.put(PlaylistEntry.COLUMN_ALBUM, cur.getString(4));
        values.put(PlaylistEntry.COLUMN_NAME, cur.getString(5));
        values.put(PlaylistEntry.COLUMN_TRACK_NUM, cur.getLong(6));
        values.put(PlaylistEntry.COLUMN_DURATION, cur.getLong(7));
        values.put(PlaylistEntry.COLUMN_ALBUM_ART_URL, cur.getString(8));
        return values;
    }

    private String buildMigrateDataFromTempSql(String tempTableName) {
        return "INSERT INTO " + TABLE_NAME + " (" +
                PlaylistEntry.COLUMN_SEQUENCE + "," +
                PlaylistEntry.COLUMN_SONG_ID + "," +
                PlaylistEntry.COLUMN_ARTIST + "," +
                PlaylistEntry.COLUMN_ALBUM + "," +
                PlaylistEntry.COLUMN_NAME + "," +
                PlaylistEntry.COLUMN_DURATION + "," +
                PlaylistEntry.COLUMN_TRACK_NUM + "," +
                PlaylistEntry.COLUMN_ALBUM_ART_URL + ")"
                + " SELECT " +
                PlaylistEntry.COLUMN_SEQUENCE + "," +
                PlaylistEntry.COLUMN_SONG_ID + "," +
                PlaylistEntry.COLUMN_ARTIST + "," +
                PlaylistEntry.COLUMN_ALBUM + "," +
                PlaylistEntry.COLUMN_NAME + "," +
                PlaylistEntry.COLUMN_DURATION + "," +
                PlaylistEntry.COLUMN_TRACK_NUM + "," +
                PlaylistEntry.COLUMN_ALBUM_ART_URL
                + " FROM " + tempTableName;
    }

    private class InitTask extends AsyncTask<Void, Void, Void> {
        private Context context;

        public InitTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            PlaylistStoreDbHelper helper = new PlaylistStoreDbHelper(context);
            db = helper.getReadableDatabase();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            initTask = null;

            if (initListener != null) {
                initListener.onInitComplete();
                initListener = null;
            }
        }
    }
}
