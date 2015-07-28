package com.lenworthrose.music.util;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.lenworthrose.music.activity.PlayingNowActivity;

public class Utils {
    public static IntentFilter createPlaybackIntentFilter() {
        IntentFilter filter = new IntentFilter(Constants.PLAYING_NOW_PLAYLIST_CHANGED);
        filter.addAction(Constants.PLAYING_NOW_CHANGED);
        filter.addAction(Constants.PLAYBACK_STATE_CHANGED);
        return filter;
    }

    public static PendingIntent createPlayingNowPendingIntentWithBackstack(Context context, int reqId) {
        Intent intent = new Intent(context, PlayingNowActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return TaskStackBuilder.create(context).addNextIntentWithParentStack(intent).getPendingIntent(reqId, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Converts a long millisecond value to a displayable time format. Calls intToTimeDisplay() internally.
     *
     * @param value time in milliseconds
     * @return String representing the time in (hh:)mm:ss format
     */
    public static String longToTimeDisplay(long value) {
        return intToTimeDisplay((int)(value / 1000));
    }

    /**
     * Converts an integer second value to a displayable time format.
     *
     * @param value time in milliseconds
     * @return String representing the time in (hh:)mm:ss format
     */
    public static String intToTimeDisplay(int value) {
        StringBuilder sb = new StringBuilder();

        if (value < 0) {
            sb.append("-");
            value = Math.abs(value);
        }

        int numSeconds = value % 60;
        int numMinutes = value / 60;
        int numHours = numMinutes / 60;

        if (numHours > 0) {
            numMinutes %= 60;
            sb.append(numHours).append(":").append(String.format("%02d", numMinutes));
        } else {
            sb.append(String.format("%d", numMinutes));
        }

        sb.append(":").append(String.format("%02d", numSeconds));

        return sb.toString();
    }

    public static short[] getCustomEqualizerLevels(SharedPreferences sharedPreferences) {
        int bandCount = sharedPreferences.getInt(Constants.SETTING_EQUALIZER_BAND_COUNT, 0);

        if (bandCount > 0) {
            short[] levels = new short[bandCount];

            for (int i = 0; i < levels.length; i++)
                levels[i] = (short)sharedPreferences.getInt(Constants.SETTING_EQUALIZER_BAND + i, 0);

            return levels;
        }

        return null;
    }

    public static void storeCustomEqualizerLevels(SharedPreferences sharedPreferences, short[] levels) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.SETTING_EQUALIZER_BAND_COUNT, levels.length);

        for (int i = 0; i < levels.length; i++)
            editor.putInt(Constants.SETTING_EQUALIZER_BAND + i, levels[i]);

        editor.apply();
    }

    public static String buildAlbumArtUrl(long albumId) {
        Uri albumArtUri = Uri.parse(Constants.EXTERNAL_ALBUM_ART_URL);
        albumArtUri = ContentUris.withAppendedId(albumArtUri, albumId);
        return albumArtUri.toString();
    }

    public static String[] getAlbumArtUrls(Context context, long artistId) {
        int i = 0;
        String[] albumArt = null;

        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Albums.ALBUM_ART },
                MediaStore.Audio.Media.ARTIST_ID + "=?",
                new String[] { String.valueOf(artistId) },
                MediaStore.Audio.Albums.ALBUM_KEY + " DESC");

        if (cursor.moveToFirst()) {
            albumArt = new String[4];

            do {
                if (!TextUtils.isEmpty(cursor.getString(0)))
                    albumArt[i++] = cursor.getString(0);
            } while (cursor.moveToNext() && i < 4);
        }

        cursor.close();
        return albumArt;
    }
}
