package com.lenworthrose.music.view;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.widget.TextView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.helper.GridViewHelper;

public class GridItem extends CheckableFrameLayout {
    private TextView label;
    private SquareImageView imgView;
    private GridViewHelper helper;

    public GridItem(Context context, GridViewHelper helper) {
        super(context);
        this.helper = helper;

        inflate(getContext(), R.layout.grid_item, this);
        setPadding(5, 5, 5, 5);
        label = (TextView)findViewById(R.id.grid_label);
        imgView = (SquareImageView)findViewById(R.id.grid_image);

        ShapeDrawable.ShaderFactory shaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(0, 0, 0, height,
                        new int[] { 0x00121212, 0x21121212, 0x3B121212, 0x52121212, 0x70121212, 0x8A121212, 0x9D121212, 0xD0121212 },
                        new float[] { 0f, .023f, .039f, .056f, .080f, .110f, .165f, 1f },
                        Shader.TileMode.CLAMP);
            }
        };

        PaintDrawable bgDrawable = new PaintDrawable();
        bgDrawable.setShape(new RectShape());
        bgDrawable.setShaderFactory(shaderFactory);
        label.setBackground(bgDrawable);
    }

    private void init() {
//        if (backgroundResourceId == 0) {
//            TypedValue value = new TypedValue();
//            getContext().getTheme().resolveAttribute(R.attr.listItemSelector, value, true);
//            backgroundResourceId = value.resourceId;
//        }

//        inflate(getContext(), R.layout.grid_item, this);
//        setBackgroundResource(backgroundResourceId);
    }

    public void setData(Cursor cursor) {
        helper.configure(this, cursor);
    }

    public void setText(String text) {
        label.setText(text);
    }

    public void setImage(Bitmap bitmap) {
        imgView.setImageBitmap(bitmap);
    }
}
