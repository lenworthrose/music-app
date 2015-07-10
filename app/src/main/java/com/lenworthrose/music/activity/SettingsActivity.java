package com.lenworthrose.music.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

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
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        getFragmentManager().beginTransaction().replace(R.id.main_content, new SettingsFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    public void onAboutClicked(MenuItem unused) {
        startActivity(new Intent(this, AboutActivity.class));
    }
}
