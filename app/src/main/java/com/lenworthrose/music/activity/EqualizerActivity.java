package com.lenworthrose.music.activity;

import android.content.ComponentName;
import android.content.Context;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.Constants;
import com.lenworthrose.music.util.Utils;

/**
 * Activity for changing the {@link Equalizer}. Available from {@link PlayingNowActivity}.
 */
public class EqualizerActivity extends AppCompatActivity implements ServiceConnection, SeekBar.OnSeekBarChangeListener,
        CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {
    private Equalizer equalizer;
    private ViewGroup container;
    private SharedPreferences sharedPreferences;
    private SeekBar[] seekBars;
    private short[] bandLevels;
    private short minLevel;
    private PresetSpinnerAdapter presetAdapter;
    private Spinner presetSpinner;

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
        int selectedPreset = presetSpinner.getSelectedItemPosition();
        sharedPreferences.edit().putInt(Constants.SETTING_EQUALIZER_PRESET, selectedPreset).apply();

        if (selectedPreset == presetAdapter.getCustomPresetIndex())
            Utils.storeCustomEqualizerLevels(sharedPreferences, bandLevels);

        unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        equalizer = ((PlaybackService.LocalBinder)service).getService().getEqualizer();
        configureEqualizerUi();
        setEqualizerBars(getBandLevelsFromEqualizer(equalizer));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) { }

    private void configureEqualizerUi() {
        int savedPreset = sharedPreferences.getInt(Constants.SETTING_EQUALIZER_PRESET, 0);
        presetAdapter = new PresetSpinnerAdapter(this, equalizer);
        presetSpinner = (Spinner)findViewById(R.id.eq_spinner);
        presetSpinner.setAdapter(presetAdapter);
        presetSpinner.setSelection(savedPreset);
        presetSpinner.post(new Runnable() {
            @Override
            public void run() {
                presetSpinner.setOnItemSelectedListener(EqualizerActivity.this);
            }
        });

        short bands = equalizer.getNumberOfBands();
        bandLevels = new short[bands];
        short maxLevel = equalizer.getBandLevelRange()[1];
        minLevel = equalizer.getBandLevelRange()[0];
        seekBars = new SeekBar[bands];

        for (short i = 0; i < bands; i++) {
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
            bar.setOnSeekBarChangeListener(this);
            bar.setTag(i);

            row.addView(minDbTextView);
            row.addView(bar);
            row.addView(maxDbTextView);

            seekBars[i] = bar;
            container.addView(row);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (presetSpinner.getSelectedItemPosition() != presetAdapter.getCustomPresetIndex()) {
            presetSpinner.setOnItemSelectedListener(null);
            presetSpinner.setSelection(presetAdapter.getCustomPresetIndex());
            presetSpinner.post(new Runnable() {
                @Override
                public void run() {
                    presetSpinner.setOnItemSelectedListener(EqualizerActivity.this);
                }
            });

            bandLevels = getBandLevelsFromBars();
        }

        short band = (Short)seekBar.getTag();
        short newLevel = (short)(progress + minLevel);
        bandLevels[band] = newLevel;
        equalizer.setBandLevel(band, newLevel);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        equalizer.setEnabled(isChecked);
        sharedPreferences.edit().putBoolean(Constants.SETTING_EQUALIZER_ENABLED, isChecked).apply();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != presetAdapter.getCustomPresetIndex()) {
            equalizer.usePreset((short)position);
            bandLevels = getBandLevelsFromEqualizer(equalizer);
        } else {
            short[] customLevels = Utils.getCustomEqualizerLevels(sharedPreferences);
            bandLevels = customLevels != null ? customLevels : new short[equalizer.getNumberOfBands()];

            for (short i = 0; i < bandLevels.length; i++)
                equalizer.setBandLevel(i, bandLevels[i]);
        }

        setEqualizerBars(bandLevels);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    private short[] getBandLevelsFromEqualizer(Equalizer equalizer) {
        short[] levels = new short[equalizer.getNumberOfBands()];

        for (short i = 0; i < levels.length; i++)
            levels[i] = equalizer.getBandLevel(i);

        return levels;
    }

    private short[] getBandLevelsFromBars() {
        short[] levels = new short[seekBars.length];

        for (int i = 0; i < seekBars.length; i++)
            levels[i] = (short)(seekBars[i].getProgress() + minLevel);

        return levels;
    }

    private void setEqualizerBars(short[] levels) {
        for (int i = 0; i < seekBars.length; i++) {
            seekBars[i].setOnSeekBarChangeListener(null);
            seekBars[i].setProgress(levels[i] - minLevel);
            seekBars[i].setOnSeekBarChangeListener(this);
        }
    }

    public void onSoundSettingsClicked(MenuItem unused) {
        startActivity(new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS));
    }

    private static class PresetSpinnerAdapter extends ArrayAdapter<String> {
        private int customPresetIndex;

        public PresetSpinnerAdapter(Context context, Equalizer equalizer) {
            super(context, android.R.layout.simple_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            short numPresets = equalizer.getNumberOfPresets();
            String[] presets = new String[numPresets + 1];

            for (short i = 0; i < numPresets; i++)
                presets[i] = equalizer.getPresetName(i);

            customPresetIndex = presets.length - 1;
            presets[customPresetIndex] = context.getString(R.string.custom);

            addAll(presets);
        }

        public int getCustomPresetIndex() {
            return customPresetIndex;
        }
    }
}
