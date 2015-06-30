package com.lenworthrose.music.util;

import android.database.Cursor;

import com.lenworthrose.music.IdType;

import java.util.ArrayList;

public interface NavigationListener {
    void onNavigate(IdType type, long id);

    void playSongs(Cursor songsCursor, int from);

    void play(IdType type, ArrayList<Long> ids);
    void add(IdType type, ArrayList<Long> ids);
    void addAsNext(IdType type, ArrayList<Long> ids);
}
