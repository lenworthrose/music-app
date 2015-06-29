package com.lenworthrose.music.util;

public class Constants {
    public static final String CMD_PLAY_ALBUM = "com.lenworthrose.music.PlayAlbum";

    public static final String PLAYING_NOW_CHANGED = "com.lenworthrose.music.PlayingNowChanged";
    public static final String PLAYING_NOW_PLAYLIST_CHANGED = "com.lenworthrose.music.PlayingNowPlaylistChanged";
    public static final String PLAYBACK_STATE_CHANGED = "com.lenworthrose.music.PlaybackStateChanged";

    public static final String TYPE = "Type";
    public static final String TYPE_ARTISTS = "Artists";
    public static final String TYPE_ALBUMS = "Albums";
    public static final String TYPE_SONGS = "Songs";
    public static final String ID = "ID";
    public static final String FROM = "From";

    public static final String SETTING_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String SETTING_AUTO_HIDE_PLAYING_NOW_OVERLAYS = "auto_hide_playing_now_overlays";

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
