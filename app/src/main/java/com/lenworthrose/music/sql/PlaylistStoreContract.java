package com.lenworthrose.music.sql;

import android.provider.BaseColumns;

/**
 * Defines the database table for the Playing Now Playlist.
 */
public final class PlaylistStoreContract {
    private PlaylistStoreContract() { }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    static final String SQL_CREATE_DEVICE_PLAYLIST = buildCreatePlaylistTableSql(SqlPlaylistStore.TABLE_NAME);
    static final String SQL_DELETE_DEVICE_PLAYLIST = "DROP TABLE IF EXISTS " + SqlPlaylistStore.TABLE_NAME;

    public static String buildCreatePlaylistTableSql(String tableName) {
        return buildCreatePlaylistTableSql(tableName, false);
    }

    public static String buildCreatePlaylistTableSql(String tableName, boolean isTemp) {
        return "CREATE " + (!isTemp ? "TABLE " : "TEMP TABLE IF NOT EXISTS ") + tableName + " (" +
                PlaylistEntry._ID + " INTEGER PRIMARY KEY," +
                PlaylistEntry.COLUMN_SEQUENCE + INTEGER_TYPE + COMMA_SEP +
                PlaylistEntry.COLUMN_SONG_ID + INTEGER_TYPE + COMMA_SEP +
                PlaylistEntry.COLUMN_ARTIST + TEXT_TYPE + COMMA_SEP +
                PlaylistEntry.COLUMN_ALBUM + TEXT_TYPE + COMMA_SEP +
                PlaylistEntry.COLUMN_NAME + TEXT_TYPE + COMMA_SEP +
                PlaylistEntry.COLUMN_DURATION + INTEGER_TYPE + COMMA_SEP +
                PlaylistEntry.COLUMN_TRACK_NUM + INTEGER_TYPE + COMMA_SEP +
                PlaylistEntry.COLUMN_FILE_URL + TEXT_TYPE + COMMA_SEP +
                PlaylistEntry.COLUMN_ALBUM_ART_URL + TEXT_TYPE + ")";
    }

    public static abstract class PlaylistEntry implements BaseColumns {
        public static final String COLUMN_SEQUENCE = "sequence";
        public static final String COLUMN_SONG_ID = "id";
        public static final String COLUMN_ARTIST = "artist";
        public static final String COLUMN_ALBUM = "album";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_DURATION = "duration";
        public static final String COLUMN_TRACK_NUM = "tracknum";
        public static final String COLUMN_FILE_URL = "fileurl";
        public static final String COLUMN_ALBUM_ART_URL = "arturl";
    }
}
