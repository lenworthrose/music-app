package com.lenworthrose.music.playback;

import android.content.Context;
import android.content.Intent;

import com.lenworthrose.music.helper.Constants;

/**
 * Created by Lenny on 2015-06-26.
 */
public class PlaybackDelegator {
    public static void playAlbum(Context context, long albumId, int from) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(Constants.CMD_PLAY_ALBUM);
        intent.putExtra(Constants.ID, albumId);
        if (from != 0) intent.putExtra(Constants.FROM, from);
        context.startService(intent);
    }
}
