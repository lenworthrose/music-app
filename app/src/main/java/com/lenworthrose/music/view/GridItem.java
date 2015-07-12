package com.lenworthrose.music.view;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.widget.ImageView;
import android.widget.TextView;

import com.lenworthrose.music.R;

/**
 * A {@link android.view.View} displayed in a {@link android.widget.GridView}. Contains a
 * 2-line {@link TextView} overlaid atop a {@link SquareImageView}.
 */
public class GridItem extends CheckableFrameLayout {
    private TextView label;
    private SquareImageView big, imgView1, imgView2, imgView3, imgView4;

    public GridItem(Context context) {
        super(context);
        setBackgroundResource(R.drawable.list_item_selector);

        inflate(getContext(), R.layout.grid_item, this);
        setPadding(5, 5, 5, 5);
        label = (TextView)findViewById(R.id.grid_label);
        big = (SquareImageView)findViewById(R.id.grid_big_image);
        imgView1 = (SquareImageView)findViewById(R.id.grid_image1);
        imgView2 = (SquareImageView)findViewById(R.id.grid_image2);
        imgView3 = (SquareImageView)findViewById(R.id.grid_image3);
        imgView4 = (SquareImageView)findViewById(R.id.grid_image4);


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

    public void setText(String text) {
        label.setText(text);
    }

    public ImageView getBigImageView() {
        return big;
    }

    public ImageView getImageView1() {
        return imgView1;
    }

    public ImageView getImageView2() {
        return imgView2;
    }

    public ImageView getImageView3() {
        return imgView3;
    }

    public ImageView getImageView4() {
        return imgView4;
    }

    public void resetImages() {
        big.setImageResource(android.R.color.transparent);
        imgView1.setImageResource(android.R.color.transparent);
        imgView2.setImageResource(android.R.color.transparent);
        imgView3.setImageResource(android.R.color.transparent);
        imgView4.setImageResource(android.R.color.transparent);
    }
}
