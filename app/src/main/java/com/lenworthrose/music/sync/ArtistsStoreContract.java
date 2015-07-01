package com.lenworthrose.music.sync;

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
            ArtistEntry._ID + " INTEGER PRIMARY KEY," +
            ArtistEntry.COLUMN_MEDIASTORE_KEY + TEXT_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_NAME + TEXT_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_NUM_ALBUMS + INTEGER_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_MUSICBRAINZ_ID + TEXT_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_BIO + TEXT_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_ARTIST_ART_FILE_URL + TEXT_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_1 + TEXT_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_2 + TEXT_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_3 + TEXT_TYPE + COMMA_SEP +
            ArtistEntry.COLUMN_ALBUM_ART_FILE_URL_4 + TEXT_TYPE + ")";
    ;
    static final String SQL_DELETE_ARTISTS = "DROP TABLE IF EXISTS artists";

    public static abstract class ArtistEntry implements BaseColumns {
        public static final String COLUMN_MEDIASTORE_KEY = "mediastorekey";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_NUM_ALBUMS = "numalbums";
        public static final String COLUMN_MUSICBRAINZ_ID = "mbid";
        public static final String COLUMN_BIO = "bio";
        public static final String COLUMN_ARTIST_ART_FILE_URL = "fileurl";
        public static final String COLUMN_ALBUM_ART_FILE_URL_1 = "albumarturl1";
        public static final String COLUMN_ALBUM_ART_FILE_URL_2 = "albumarturl2";
        public static final String COLUMN_ALBUM_ART_FILE_URL_3 = "albumarturl3";
        public static final String COLUMN_ALBUM_ART_FILE_URL_4 = "albumarturl4";
    }
}
