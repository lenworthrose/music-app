package com.lenworthrose.music.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.lenworthrose.music.R;

/**
 * Activity that displays information about the app (version, licensing, etc.). Accessible from {@link SettingsActivity}.
 */
public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_about);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {
            ((TextView)findViewById(R.id.about_version)).setText(getString(R.string.version_info, getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (PackageManager.NameNotFoundException ex) {
            findViewById(R.id.about_version).setVisibility(View.GONE);
            Log.e("AboutActivity", "Unable to get version info for About page.", ex);
        }

        ((TextView)findViewById(R.id.about_link1)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)findViewById(R.id.about_link2)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)findViewById(R.id.about_link3)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)findViewById(R.id.about_link4)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)findViewById(R.id.about_link5)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)findViewById(R.id.about_link6)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = NavUtils.getParentActivityIntent(this);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            NavUtils.navigateUpTo(this, intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
