package com.lenworthrose.music.adapter;

import android.content.Context;

import com.lenworthrose.music.IdType;
import com.lenworthrose.music.util.Constants;

/**
 * Factory to create {@link BaseSwitchableAdapter} instances based on the supplied {@link IdType}.
 */
public class AdapterFactory {
    public static BaseSwitchableAdapter createAdapter(Context context, boolean isGrid, IdType type, long id) {
        if (id == Constants.ALL) { //Root instance. Return Adapter for viewing all of the specified IdType
            switch (type) {
                case ARTIST:
                    return new ArtistsAdapter(context, isGrid);
                case ALBUM:
                    return new AlbumsAdapter(context, isGrid);
                case SONG:
                    return new SongsAdapter(context, isGrid);
                default:
                    throw new IllegalArgumentException("Unsupported IdType");
            }
        } else {
            switch (type) { //Drilling into a particular item of type IdType. Return an Adapter to show children.
                case ARTIST:
                case GENRE:
                    return new AlbumsAdapter(context, isGrid, type, id);
                case ALBUM:
                case PLAYLIST:
                    return new SongsAdapter(context, isGrid, type, id);
            }
        }

        throw new IllegalArgumentException("Unsupported IdType in createAdapter: " + type);
    }
}
