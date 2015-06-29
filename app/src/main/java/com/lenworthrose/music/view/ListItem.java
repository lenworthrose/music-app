package com.lenworthrose.music.view;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lenworthrose.music.R;

public class ListItem extends CheckableFrameLayout {
    private int position = -1;
    private String key;
    private ImageView dragHandle;
    private SquareImageView image;
    private TextView title, subtitle, status;
    private boolean isEditModeEnabled;
    private boolean showCoverArt;

    public ListItem(Context context) {
        super(context);

        inflate(context, R.layout.list_item, this);
        title = (TextView)findViewById(R.id.list_item_title);
        subtitle = (TextView)findViewById(R.id.list_item_subtitle);
        status = (TextView)findViewById(R.id.list_item_status);
        dragHandle = (ImageView)findViewById(R.id.list_item_drag_handle);
        image = (SquareImageView)findViewById(R.id.list_item_image);
    }

    public void setData(Cursor cursor) {
        position = cursor.getPosition();
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    public void setSubtitle(String text) {
        subtitle.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
        subtitle.setText(text);
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    public void setImage(Bitmap bitmap) {
        if (bitmap != null) {
            image.setVisibility(View.VISIBLE);
            image.setImageBitmap(bitmap);
        } else {
            image.setVisibility(View.GONE);
        }
    }
}
