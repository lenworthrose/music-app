package com.lenworthrose.music.view;

import android.content.Context;
import android.database.Cursor;
import android.widget.ImageView;
import android.widget.TextView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.helper.ListViewHelper;

public class ListItem extends CheckableFrameLayout {
    private int position = -1;
    private String key;
    private ImageView dragHandle;
    private SquareImageView image;
    private TextView title, status;
    private boolean isEditModeEnabled;
    private boolean showCoverArt;
    private ListViewHelper helper;

    public ListItem(Context context, ListViewHelper helper) {
        super(context);
        this.helper = helper;

        inflate(context, R.layout.list_item, this);
        title = (TextView)findViewById(R.id.list_item_title);
        status = (TextView)findViewById(R.id.list_item_status);
        dragHandle = (ImageView)findViewById(R.id.list_item_drag_handle);
        image = (SquareImageView)findViewById(R.id.list_item_image);
    }

    public void setData(Cursor cursor) {
        position = cursor.getPosition();
        helper.configure(this, cursor);
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    public void setStatus(String text) {
        status.setText(text);
    }
}
