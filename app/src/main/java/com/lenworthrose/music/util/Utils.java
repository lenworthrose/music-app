package com.lenworthrose.music.util;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

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

    public interface BitmapCallback {
        void onBitmapReady(Bitmap bitmap);
    }

    public static void createDropShadowBitmap(Bitmap bitmap, final BitmapCallback callback) {
        AsyncTask<Bitmap, Void, Bitmap> task = new AsyncTask<Bitmap, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Bitmap... params) {
                Bitmap bitmap = params[0];

                if (bitmap.getByteCount() > 10 * 1024 * 1024) //Abort! Image is too big, this could explode the app's memory.
                    return bitmap;

                BlurMaskFilter blurFilter;

                try {
                    blurFilter = new BlurMaskFilter(bitmap.getWidth() / 80, BlurMaskFilter.Blur.OUTER);
                } catch (IllegalArgumentException ex) {
                    Log.w("PlayingItemFragment", "IllegalArgumentException occurred creating BlurMaskFilter: " + ex.getMessage(), ex);
                    return bitmap;
                }

                Paint shadowPaint = new Paint();
                shadowPaint.setMaskFilter(blurFilter);

                int[] offsetXY = new int[2];
                Bitmap shadowImage = bitmap.extractAlpha(shadowPaint, offsetXY);
                Bitmap shadowImage32 = shadowImage.copy(Bitmap.Config.ARGB_8888, true);
                shadowImage.recycle();
                setPremultiplied(shadowImage32);

                Canvas c = new Canvas(shadowImage32);
                c.drawBitmap(bitmap, -offsetXY[0], -offsetXY[1], null);

                return shadowImage32;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                callback.onBitmapReady(bitmap);
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
    }

    @TargetApi(19)
    private static void setPremultiplied(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 19 && !bitmap.isPremultiplied())
            bitmap.setPremultiplied(true);
    }

    public static void createBlurredBitmap(Bitmap bitmap, final BitmapCallback callback) {
        AsyncTask<Bitmap, Void, Bitmap> task = new AsyncTask<Bitmap, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Bitmap... params) {
                Bitmap bitmap = params[0];

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                boolean didScale = false;

                if (width > 400) { //always create a 400px wide image
                    double ratio = (double)400 / (double)width;
                    width = 400;
                    height *= ratio;
                    didScale = true;
                }

                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, false);

                if (scaled == null) {
                    return bitmap;
                }

                Bitmap retVal = fastblur(scaled, 14);
                if (didScale) scaled.recycle();

                return retVal;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                callback.onBitmapReady(bitmap);
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
    }

    private static Bitmap fastblur(Bitmap sentBitmap, int radius) {
        // Stack Blur v1.0 from
        // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
        //
        // Java Author: Mario Klingemann <mario at quasimondo.com>
        // http://incubator.quasimondo.com
        // created Feburary 29, 2004
        // Android port : Yahel Bouaziz <yahel at kayenko.com>
        // http://www.kayenko.com
        // ported april 5th, 2012

        // This is a compromise between Gaussian Blur and Box blur
        // It creates much better looking blurs than Box Blur, but is
        // 7x faster than my Gaussian Blur implementation.
        //
        // I called it Stack Blur because this describes best how this
        // filter works internally: it creates a kind of moving stack
        // of colors whilst scanning through the image. Thereby it
        // just has to add one new block of color to the right side
        // of the stack and remove the leftmost color. The remaining
        // colors on the topmost layer of the stack are either added on
        // or reduced by one, depending on if they are on the right or
        // on the left side of the stack.
        //
        // If you are using this algorithm in your code please add
        // the following line:
        //
        // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return null;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return (bitmap);
    }

    public static String buildAlbumArtUrl(long albumId) {
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        albumArtUri = ContentUris.withAppendedId(albumArtUri, albumId);
        return albumArtUri.toString();
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
