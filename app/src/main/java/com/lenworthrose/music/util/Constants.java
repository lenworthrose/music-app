package com.lenworthrose.music.util;

/**
 * This class defines constants and enums used throughout the application.
 */
public class Constants {
    public static final String CMD_PLAY_PAUSE = "com.lenworthrose.music.PlayPause";
    public static final String CMD_STOP = "com.lenworthrose.music.Stop";
    public static final String CMD_NEXT = "com.lenworthrose.music.Next";
    public static final String CMD_PREVIOUS = "com.lenworthrose.music.Previous";
    public static final String CMD_ACTIVITY_STARTING = "com.lenworthrose.music.ActivityStarting";
    public static final String CMD_ACTIVITY_CLOSING = "com.lenworthrose.music.ActivityClosing";

    // Actions Broadcast by PlaybackService when changes occur.
    public static final String PLAYING_NOW_CHANGED = "com.lenworthrose.music.PlayingNowChanged";
    public static final String EXTRA_ARTIST = "Artist";
    public static final String EXTRA_ALBUM = "Album";
    public static final String EXTRA_TITLE = "Title";
    public static final String EXTRA_TRACK_NUM = "TrackNum";
    public static final String EXTRA_PLAYLIST_POSITION = "PlaylistPosition";
    public static final String EXTRA_PLAYLIST_TOTAL_TRACKS = "PlaylistTotalTracks";
    public static final String EXTRA_ALBUM_ART_URL = "AlbumArtUri";

    public static final String PLAYING_NOW_PLAYLIST_CHANGED = "com.lenworthrose.music.PlayingNowPlaylistChanged";

    public static final String PLAYBACK_STATE_CHANGED = "com.lenworthrose.music.PlaybackStateChanged";
    public static final String EXTRA_STATE = "State";
    public static final String EXTRA_DURATION = "Duration";

    public static final String PLAYBACK_MODIFICATION_COMPLETE = "com.lenworthrose.music.PlaybackModificationComplete";
    public static final String EXTRA_MODIFICATION_TYPE = "WHICH";
    public static final int EXTRA_MODIFICATION_TYPE_PLAY = 0;
    public static final int EXTRA_MODIFICATION_TYPE_ADD = 1;
    public static final int EXTRA_MODIFICATION_TYPE_ADD_AS_NEXT = 2;

    public static final String TYPE = "Type";
    public static final String ID = "ID";

    public static final String SETTING_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String SETTING_AUTO_HIDE_PLAYING_NOW_OVERLAYS = "auto_hide_playing_now_overlays";
    public static final String SETTING_REPEAT_MODE = "repeat_mode";
    public static final String SETTING_START_LOCATION = "start_location";

    public static final long ALL = Long.MIN_VALUE;

    public enum PlaybackState {
        STOPPED,
        PAUSED,
        PLAYING,
        BUFFERING
    }

    public enum RepeatMode {
        OFF,
        PLAYLIST,
        TRACK,
        STOP
    }
}
