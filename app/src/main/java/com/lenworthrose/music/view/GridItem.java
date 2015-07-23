package com.lenworthrose.music.view;

import android.content.Context;
import android.graphics.drawable.PaintDrawable;
import android.widget.ImageView;
import android.widget.TextView;

import com.lenworthrose.music.R;
import com.lenworthrose.music.view.GradientDrawable.Type;

/**
 * A {@link android.view.View} displayed in a {@link android.widget.GridView}. Contains a
 * 2-line {@link TextView} overlaid atop a {@link SquareImageView}.
 */
public class GridItem extends CheckableFrameLayout {
    private TextView label;
    private SquareImageView big, imgView1, imgView2, imgView3, imgView4;
    private PaintDrawable normalBackground, checkedBackground;
    private int normalTextColor, checkedTextColor;

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
        normalBackground = new GradientDrawable(Type.GRID_ITEM, getResources().getColor(R.color.grid_item_label_background));
        checkedBackground = new GradientDrawable(Type.GRID_ITEM, getResources().getColor(R.color.colorAccentDark));
        label.setBackground(normalBackground);
        normalTextColor = getResources().getColor(R.color.textSecondary);
        checkedTextColor = getResources().getColor(R.color.textPrimary);
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

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (isChecked()) {
            label.setBackground(checkedBackground);
            label.setTextColor(checkedTextColor);
        } else {
            label.setBackground(normalBackground);
            label.setTextColor(normalTextColor);
        }
    }
}
