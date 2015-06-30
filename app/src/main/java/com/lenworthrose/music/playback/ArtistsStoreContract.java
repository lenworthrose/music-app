package com.lenworthrose.music.playback;

import android.provider.BaseColumns;

/**
 * Defines the DB table for storing information about Artists.
 */
public class ArtistsStoreContract {
    private ArtistsStoreContract() { }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    static final String SQL_CREATE_ARTISTS = "CREATE TABLE artists (" +
            PlaylistEntry._ID + " INTEGER PRIMARY KEY," +
            PlaylistEntry.COLUMN_MEDIASTORE_ID + INTEGER_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_NAME + TEXT_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_NUM_ALBUMS + INTEGER_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_MUSICBRAINZ_ID + TEXT_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_MUSICBRAINZ_WIKI_INFO + TEXT_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_ARTIST_ART_FILE_URL + TEXT_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_ALBUM_ART_FILE_URL_1 + TEXT_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_ALBUM_ART_FILE_URL_2 + TEXT_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_ALBUM_ART_FILE_URL_3 + TEXT_TYPE + COMMA_SEP +
            PlaylistEntry.COLUMN_ALBUM_ART_FILE_URL_4 + TEXT_TYPE + ")";
    ;
    static final String SQL_DELETE_ARTISTS = "DROP TABLE IF EXISTS artists";

    public static abstract class PlaylistEntry implements BaseColumns {
        public static final String COLUMN_MEDIASTORE_ID = "mediastore_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_NUM_ALBUMS = "numalbums";
        public static final String COLUMN_MUSICBRAINZ_ID = "mbid";
        public static final String COLUMN_MUSICBRAINZ_WIKI_INFO = "info";
        public static final String COLUMN_ARTIST_ART_FILE_URL = "fileurl";
        public static final String COLUMN_ALBUM_ART_FILE_URL_1 = "albumarturl1";
        public static final String COLUMN_ALBUM_ART_FILE_URL_2 = "albumarturl2";
        public static final String COLUMN_ALBUM_ART_FILE_URL_3 = "albumarturl3";
        public static final String COLUMN_ALBUM_ART_FILE_URL_4 = "albumarturl4";
    }
}
