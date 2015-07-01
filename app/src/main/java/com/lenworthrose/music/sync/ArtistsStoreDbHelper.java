package com.lenworthrose.music.sync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ArtistsStoreDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Artists.db";
    private static final int DATABASE_VERSION = 1;

    public ArtistsStoreDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ArtistsStoreContract.SQL_CREATE_ARTISTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(ArtistsStoreContract.SQL_DELETE_ARTISTS);
        onCreate(db);
    }
}
