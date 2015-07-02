package com.lenworthrose.music.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lenworthrose.music.R;
import com.lenworthrose.music.fragment.SettingsFragment;

/**
 * Activity to show Settings.
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getFragmentManager().beginTransaction().replace(R.id.main_content, new SettingsFragment()).commit();
    }
}
