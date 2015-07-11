package com.lenworthrose.music.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Utils;

/**
 * Activity for changing the {@link Equalizer}. Available from {@link PlayingNowActivity}.
 */
public class EqualizerActivity extends AppCompatActivity implements ServiceConnection, SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {
    private Equalizer equalizer;
    private ViewGroup container;
    private SharedPreferences sharedPreferences;
    private short[] bandLevels;
    private short minLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_equalizer);
        container = (ViewGroup)findViewById(R.id.eq_container);
        CheckBox enabled = (CheckBox)findViewById(R.id.eq_enabled);
        enabled.setChecked(sharedPreferences.getBoolean(Constants.SETTING_EQUALIZER_ENABLED, false));
        enabled.setOnCheckedChangeListener(this);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bindService(new Intent(this, PlaybackService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_equalizer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        Utils.storeEqualizerSettings(sharedPreferences, bandLevels);
        unbindService(this);
        super.onDestroy();
    }

    private void configureEqualizerUi() {
        short bands = equalizer.getNumberOfBands();
        minLevel = equalizer.getBandLevelRange()[0];
        short maxLevel = equalizer.getBandLevelRange()[1];
        bandLevels = new short[bands];

        for (short i = 0; i < bands; i++) {
            bandLevels[i] = equalizer.getBandLevel(i);

            TextView freqTextView = new TextView(this);
            freqTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            freqTextView.setText((equalizer.getCenterFreq(i) / 1000) + "Hz");
            container.addView(freqTextView);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView minDbTextView = new TextView(this);
            minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            minDbTextView.setText((minLevel / 100) + " dB");

            TextView maxDbTextView = new TextView(this);
            maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            maxDbTextView.setText((maxLevel / 100) + " dB");

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.weight = 1;

            SeekBar bar = new SeekBar(this);
            bar.setLayoutParams(layoutParams);
            bar.setMax(maxLevel - minLevel);
            bar.setProgress(bandLevels[i] - minLevel);
            bar.setOnSeekBarChangeListener(this);
            bar.setTag(i);

            row.addView(minDbTextView);
            row.addView(bar);
            row.addView(maxDbTextView);

            container.addView(row);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        equalizer = ((PlaybackService.LocalBinder)service).getService().getEqualizer();
        equalizer.setEnabled(sharedPreferences.getBoolean(Constants.SETTING_EQUALIZER_ENABLED, false));
        configureEqualizerUi();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) { }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        short band = (Short)seekBar.getTag();
        minLevel = equalizer.getBandLevelRange()[0];
        short newLevel = (short)(progress + minLevel);
        bandLevels[band] = newLevel;
        equalizer.setBandLevel(band, newLevel);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        equalizer.setEnabled(isChecked);
        sharedPreferences.edit().putBoolean(Constants.SETTING_EQUALIZER_ENABLED, isChecked).apply();
    }

    public void onSoundSettingsClicked(MenuItem unused) {
        startActivity(new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS));
    }
}
