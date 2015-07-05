package com.lenworthrose.music.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.lenworthrose.music.R;
import com.lenworthrose.music.util.Constants;

/**
 * Service responsible for updating Widgets.
 */
public class WidgetService extends Service {
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
                String artist = intent.getStringExtra(Constants.EXTRA_ARTIST);

                for (int appWidgetId : appWidgetIds) {
                    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_view);
                    rv.setTextViewText(R.id.widget_title, intent.getStringExtra(Constants.EXTRA_TITLE));
                    rv.setTextViewText(R.id.widget_subtitle, artist);
                    rv.setViewVisibility(R.id.widget_subtitle, artist == null || artist.isEmpty() ? View.GONE : View.VISIBLE);
                    appMan.partiallyUpdateAppWidget(appWidgetId, rv);

                    Glide.with(context).load(intent.getStringExtra(Constants.EXTRA_ALBUM_ART_URL)).asBitmap().fallback(R.drawable.logo)
                            .error(R.drawable.logo).into(new AppWidgetTarget(WidgetService.this, rv, R.id.widget_image, appWidgetIds));
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.PLAYING_NOW_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) {
            //TODO: how do I get the initial content to display? Don't want to bind to service...
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }
}
