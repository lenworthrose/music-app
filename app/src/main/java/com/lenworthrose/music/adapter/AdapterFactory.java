package com.lenworthrose.music.adapter;

import android.content.Context;

import com.lenworthrose.music.IdType;

/**
 * Factory to create {@link BaseSwitchableAdapter} instances based on the supplied {@link IdType}.
 */
public class AdapterFactory {
    public static BaseSwitchableAdapter createAdapter(Context context, boolean isGrid, IdType type, long id) {
        if (type == null) return new ArtistsAdapter(context, isGrid);

        switch (type) {
            case ARTIST:
            case GENRE:
                return new AlbumsAdapter(context, isGrid, type, id);
            case ALBUM:
            case PLAYLIST:
                return new SongsAdapter(context, isGrid, type, id);
        }

        throw new IllegalArgumentException("Unsupported IdType in createAdapter: " + type);
    }
}
