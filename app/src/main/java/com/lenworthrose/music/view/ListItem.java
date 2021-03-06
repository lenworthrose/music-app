package com.lenworthrose.music.view;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lenworthrose.music.R;

/**
 * A {@link android.view.View} displayed in a {@link android.widget.ListView}. Contains
 * an optional small thumbnail, a title, subtitle, and status text.
 */
public class ListItem extends CheckableFrameLayout {
    private ImageView dragHandle;
    private SquareImageView image;
    private TextView title, subtitle, status;

    public ListItem(Context context) {
        super(context);
        setBackgroundResource(R.drawable.list_item_selector);

        inflate(context, R.layout.list_item, this);
        title = (TextView)findViewById(R.id.list_item_title);
        subtitle = (TextView)findViewById(R.id.list_item_subtitle);
        status = (TextView)findViewById(R.id.list_item_status);
        dragHandle = (ImageView)findViewById(R.id.list_item_drag_handle);
        image = (SquareImageView)findViewById(R.id.list_item_image);
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

    public ImageView getImageView() {
        return image;
    }

    public void setImageVisible(boolean visible) {
        image.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setEditModeEnabled(boolean isEditModeEnabled) {
        dragHandle.setVisibility(isEditModeEnabled ? VISIBLE : GONE);
    }
}
