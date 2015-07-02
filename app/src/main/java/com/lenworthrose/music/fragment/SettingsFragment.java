package com.lenworthrose.music.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.lenworthrose.music.R;
import com.lenworthrose.music.sync.MediaStoreService;

/**
 * Fragment to show Settings.
 */
public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        findPreference("refresh_artists").setOnPreferenceClickListener(this);
        findPreference("refresh_albums").setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String action = null;

        switch (preference.getKey()) {
            case "refresh_artists":
                action = MediaStoreService.ACTION_SYNC_WITH_MEDIA_STORE;
                break;
            case "refresh_albums":
                action = MediaStoreService.ACTION_UPDATE_ALBUMS;
                break;
        }

        if (action != null) {
            Intent intent = new Intent(getActivity(), MediaStoreService.class);
            intent.setAction(action);
            getActivity().startService(intent);
        }

        return false;
    }
}
