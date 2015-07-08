package com.lenworthrose.music.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lenworthrose.music.R;

/**
 * The {@link android.widget.Adapter} for MainActivity's navigation drawer.
 */
public class NavigationDrawerAdapter extends BaseAdapter {
    private Context context;
    private String[] items;

    public NavigationDrawerAdapter(Context context) {
        this.context = context;
        items = context.getResources().getStringArray(R.array.navigation_drawer_items);
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public String getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.navigation_drawer_list_item, parent, false);
            convertView.setTag(R.id.nav_title, convertView.findViewById(R.id.nav_title));
            convertView.setTag(R.id.nav_image, convertView.findViewById(R.id.nav_image));
            convertView.setTag(R.id.nav_divider, convertView.findViewById(R.id.nav_divider));
        }

        TextView title = (TextView)convertView.getTag(R.id.nav_title);
        title.setText(getItem(position));

        int drawableId = 0;

        switch (position) {
            case 0:
                drawableId = R.drawable.artists;
                break;
            case 1:
                drawableId = R.drawable.albums;
                break;
            case 2:
                drawableId = R.drawable.songs;
                break;
            case 3:
                drawableId = R.drawable.search;
                ((View)convertView.getTag(R.id.nav_divider)).setVisibility(View.VISIBLE);
                break;
            case 4:
                drawableId = R.drawable.playing_now;
                break;
            case 5:
                drawableId = R.drawable.settings;
                break;
        }

        ((ImageView)convertView.getTag(R.id.nav_image)).setImageResource(drawableId);

        return convertView;
    }
}
