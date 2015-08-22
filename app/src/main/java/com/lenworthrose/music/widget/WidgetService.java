package com.lenworthrose.music.widget;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.Constants;

/**
 * Service responsible for updating Widgets.
 */
public class WidgetService extends Service implements ServiceConnection {
    public static final String ACTION_REFRESH = "com.lenworthrose.music.widget.WidgetService.REFRESH";

    private BroadcastReceiver receiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        receiver = new PlaybackBroadcastReceiver();
        IntentFilter filter = new IntentFilter(Constants.PLAYING_NOW_CHANGED);
        filter.addAction(Constants.PLAYBACK_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        bindService(new Intent(this, PlaybackService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_REFRESH.equals(intent.getAction()))
            bindService(new Intent(this, PlaybackService.class), this, BIND_AUTO_CREATE);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        PlaybackService playbackService = ((PlaybackService.LocalBinder)service).getService();
        receiver.onReceive(this, playbackService.getPlayingItemIntent());
        receiver.onReceive(this, playbackService.getPlaybackStateIntent());
        unbindService(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) { }
}
