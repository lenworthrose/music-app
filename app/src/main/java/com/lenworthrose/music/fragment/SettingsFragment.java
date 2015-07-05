package com.lenworthrose.music.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.lenworthrose.music.R;
import com.lenworthrose.music.sync.MediaStoreService;
import com.lenworthrose.music.util.Constants;

/**
 * Fragment to show Settings.
 */
public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private final String REFRESH_ARTISTS = "refresh_artists";
    private final String REFRESH_ALBUMS = "refresh_albums";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        findPreference(REFRESH_ARTISTS).setOnPreferenceClickListener(this);
        findPreference(REFRESH_ALBUMS).setOnPreferenceClickListener(this);
        updateListPreferenceSummary(Constants.SETTING_START_LOCATION, R.array.start_locations_ids, R.array.start_locations, "0");
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String action = null;

        switch (preference.getKey()) {
            case REFRESH_ARTISTS:
                action = MediaStoreService.ACTION_SYNC_WITH_MEDIA_STORE;
                break;
            case REFRESH_ALBUMS:
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

    private void updateListPreferenceSummary(String preference, int valuesArrayResourceId, int displayArrayResourceId, String defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int index = 0;
        String value = prefs.getString(preference, defaultValue);
        String[] values = getResources().getStringArray(valuesArrayResourceId);

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                index = i;
                break;
            }
        }

        String summary = getResources().getStringArray(displayArrayResourceId)[index];
        if (summary.endsWith("%")) summary += "%";
        findPreference(preference).setSummary(summary);
    }
}
