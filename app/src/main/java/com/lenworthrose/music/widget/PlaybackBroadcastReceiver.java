package com.lenworthrose.music.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.lenworthrose.music.R;
import com.lenworthrose.music.util.Constants;

/**
 * {@link BroadcastReceiver} used by the {@link WidgetService} to listen for Playback Broadcasts.
 */
class PlaybackBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final AppWidgetManager appMan = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = appMan.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));

        switch (intent.getAction()) {
            case Constants.PLAYING_NOW_CHANGED:
                String artist = intent.getStringExtra(Constants.EXTRA_ARTIST);
                String title = intent.getStringExtra(Constants.EXTRA_TITLE);
                if (TextUtils.isEmpty(title)) title = context.getString(R.string.app_name);

                for (int appWidgetId : appWidgetIds) {
                    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_view);
                    rv.setTextViewText(R.id.widget_title, title);
                    rv.setTextViewText(R.id.widget_subtitle, artist);
                    rv.setViewVisibility(R.id.widget_subtitle, TextUtils.isEmpty(artist) ? View.GONE : View.VISIBLE);
                    WidgetProvider.setOnClickIntents(context, rv);
                    appMan.partiallyUpdateAppWidget(appWidgetId, rv);

                    Glide.with(context).load(intent.getStringExtra(Constants.EXTRA_ALBUM_ART_URL)).asBitmap().fallback(R.drawable.logo)
                            .error(R.drawable.logo).into(new AppWidgetTarget(context, rv, R.id.widget_image, appWidgetIds));
                }

                break;
            case Constants.PLAYBACK_STATE_CHANGED:
                Constants.PlaybackState state = (Constants.PlaybackState)intent.getSerializableExtra(Constants.EXTRA_STATE);
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
}
