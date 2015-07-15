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
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Constants.PlaybackState;

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

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final AppWidgetManager appMan = AppWidgetManager.getInstance(context);
                final int[] appWidgetIds = appMan.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));

                switch (intent.getAction()) {
                    case Constants.PLAYING_NOW_CHANGED:
                        String artist = intent.getStringExtra(Constants.EXTRA_ARTIST);

                        for (int appWidgetId : appWidgetIds) {
                            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_view);
                            rv.setTextViewText(R.id.widget_title, intent.getStringExtra(Constants.EXTRA_TITLE));
                            rv.setTextViewText(R.id.widget_subtitle, artist);
                            rv.setViewVisibility(R.id.widget_subtitle, TextUtils.isEmpty(artist) ? View.GONE : View.VISIBLE);
                            WidgetProvider.setOnClickIntents(context, rv);
                            appMan.partiallyUpdateAppWidget(appWidgetId, rv);

                            Glide.with(context).load(intent.getStringExtra(Constants.EXTRA_ALBUM_ART_URL)).asBitmap().fallback(R.drawable.logo)
                                    .error(R.drawable.logo).into(new AppWidgetTarget(WidgetService.this, rv, R.id.widget_image, appWidgetIds));
                        }

                        break;
                    case Constants.PLAYBACK_STATE_CHANGED:
                        PlaybackState state = (PlaybackState)intent.getSerializableExtra(Constants.EXTRA_STATE);
                        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_view);
                        WidgetProvider.setOnClickIntents(context, rv);

                        switch (state) {
                            case PLAYING:
                                rv.setImageViewResource(R.id.widget_play_pause, R.drawable.pause);
                                break;
                            default:
                                rv.setImageViewResource(R.id.widget_play_pause, R.drawable.play);
                                break;
                        }

                        for (int appWidgetId : appWidgetIds)
                            appMan.partiallyUpdateAppWidget(appWidgetId, rv);

                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter(Constants.PLAYING_NOW_CHANGED);
        filter.addAction(Constants.PLAYBACK_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
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
