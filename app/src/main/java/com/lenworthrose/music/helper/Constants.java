package com.lenworthrose.music.helper;

public class Constants {
    public static final String CMD_PLAY_ALBUM = "com.lenworthrose.music.PlayAlbum";

    public static final String TYPE = "Type";
    public static final String TYPE_ARTISTS = "Artists";
    public static final String TYPE_ALBUMS = "Albums";
    public static final String TYPE_SONGS = "Songs";
    public static final String ID = "ID";
    public static final String FROM = "From";

    public enum PlaybackState {
        Stopped,
        Paused,
        Playing,
        Buffering //3 = Buffering || Aborting, Please wait
    }

    public enum RepeatMode {
        Off,
        Playlist,
        Track,
        Stop
    }
}
