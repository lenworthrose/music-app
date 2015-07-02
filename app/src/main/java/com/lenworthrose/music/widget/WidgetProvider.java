package com.lenworthrose.music.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Utils;

/**
 * Provides the Widget.
 */
public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        context.startService(new Intent(context, WidgetService.class));
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        AppWidgetManager appMan = AppWidgetManager.getInstance(context);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_view);
            setOnClickIntents(context, rv);
            appMan.updateAppWidget(appWidgetId, rv);
        }

        Intent intent = new Intent(context, WidgetService.class);
        intent.setAction(WidgetService.ACTION_REFRESH);
        context.startService(intent);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        context.stopService(new Intent(context, WidgetService.class));
    }

    public static void setOnClickIntents(Context context, RemoteViews rv) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(Constants.CMD_PLAY_PAUSE);
        PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);
        rv.setOnClickPendingIntent(R.id.widget_play_pause, pending);

        intent = new Intent(context, PlaybackService.class);
        intent.setAction(Constants.CMD_PREVIOUS);
        pending = PendingIntent.getService(context, 0, intent, 0);
        rv.setOnClickPendingIntent(R.id.widget_previous, pending);

        intent = new Intent(context, PlaybackService.class);
        intent.setAction(Constants.CMD_NEXT);
        pending = PendingIntent.getService(context, 0, intent, 0);
        rv.setOnClickPendingIntent(R.id.widget_next, pending);

        intent = new Intent(context, PlaybackService.class);
        intent.setAction(Constants.CMD_STOP);
        pending = PendingIntent.getService(context, 0, intent, 0);
        rv.setOnClickPendingIntent(R.id.widget_stop, pending);

        pending = Utils.createPlayingNowPendingIntentWithBackstack(context, 667);
        rv.setOnClickPendingIntent(R.id.widget_image, pending);
    }
}
