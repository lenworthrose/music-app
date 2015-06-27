package com.lenworthrose.music.playback;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PlaylistStoreDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Playlist.db";
    public static final int DATABASE_VERSION = 1;

    public PlaylistStoreDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PlaylistStoreContract.SQL_CREATE_DEVICE_PLAYLIST);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Might be better to check the version to see if the list is compatible. But this isn't a huge deal.
        db.execSQL(PlaylistStoreContract.SQL_DELETE_DEVICE_PLAYLIST);
        onCreate(db);
    }
}
