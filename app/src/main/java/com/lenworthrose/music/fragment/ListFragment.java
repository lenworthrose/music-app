package com.lenworthrose.music.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.BaseSwitchableAdapter;
import com.lenworthrose.music.adapter.SongsAdapter;
import com.lenworthrose.music.helper.Constants;
import com.lenworthrose.music.helper.MultiSelectListener;

public class ListFragment extends Fragment {
    private String type;
    private BaseSwitchableAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) savedInstanceState = getArguments();
        type = savedInstanceState.getString(Constants.TYPE);
        long albumId = savedInstanceState.getLong(Constants.ID);

        switch (type) {
            case Constants.TYPE_SONGS:
                adapter = new SongsAdapter(getActivity(), false, albumId);
                break;
        }

        getLoaderManager().initLoader(0, null, adapter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = (ListView)view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(adapter);
        listView.setMultiChoiceModeListener(new MultiSelectListener(type));
    }

    public static ListFragment songsInstance(long albumId) {
        Bundle b = new Bundle();
        b.putString(Constants.TYPE, Constants.TYPE_SONGS);
        b.putLong(Constants.ID, albumId);

        ListFragment fragment = new ListFragment();
        fragment.setArguments(b);
        return fragment;
    }
}
