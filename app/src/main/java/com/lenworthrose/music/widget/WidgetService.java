package com.lenworthrose.music.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.playback.PlayingItem;
import com.lenworthrose.music.util.Constants;

/**
 * Service responsible for updating Widgets.
 */
public class WidgetService extends Service implements ServiceConnection {
    public static final String ACTION_REFRESH = "com.lenworthrose.music.widget.WidgetService.REFRESH";

    private BroadcastReceiver receiver;

    private void bindToPlaybackService() {
        bindService(new Intent(this, PlaybackService.class), WidgetService.this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (receiver == null ) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    bindToPlaybackService();
                }
            };

            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.PLAYING_NOW_CHANGED));
        }

        if (intent != null && ACTION_REFRESH.equals(intent.getAction()))
            bindToPlaybackService();

        return START_STICKY;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        final PlaybackService playbackService = ((PlaybackService.LocalBinder)service).getService();
        PlayingItem item = playbackService.getPlayingItem();

        final AppWidgetManager appMan = AppWidgetManager.getInstance(playbackService);
        final int[] appWidgetIds = appMan.getAppWidgetIds(new ComponentName(playbackService, WidgetProvider.class));
        String artist = item.getArtist();

        for (int appWidgetId : appWidgetIds) {
            RemoteViews rv = new RemoteViews(playbackService.getPackageName(), R.layout.widget_view);
            rv.setTextViewText(R.id.widget_title, item.getTitle());
            rv.setTextViewText(R.id.widget_subtitle, artist);
            rv.setViewVisibility(R.id.widget_subtitle, artist == null || artist.isEmpty() ? View.GONE : View.VISIBLE);
            appMan.partiallyUpdateAppWidget(appWidgetId, rv);

            Glide.with(playbackService).load(item.getAlbumArtUrl()).asBitmap().fallback(R.drawable.logo)
                    .error(R.drawable.logo).into(new AppWidgetTarget(this, rv, R.id.widget_image, appWidgetIds));

            WidgetService.this.unbindService(this);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) { }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }
}
