package com.lenworthrose.music.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.FrameLayout;

/**
 * A {@link FrameLayout} that handles the Checked state. Used by views that need to show this state,
 * e.g. during multi-select, or to show the currently playing track in
 * {@link com.lenworthrose.music.fragment.PlayingNowPlaylistFragment}.
 */
public class CheckableFrameLayout extends FrameLayout implements Checkable {
    public static final int[] CHECKED_STATE = { android.R.attr.state_checked };
    private boolean isChecked;

    public CheckableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CheckableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableFrameLayout(Context context) {
        super(context);
    }

    /*
     * @see android.widget.Checkable#isChecked()
     */
    @Override
    public boolean isChecked() {
        return isChecked;
    }

    /*
     * @see android.widget.Checkable#setChecked(boolean)
     */
    @Override
    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
        refreshDrawableState();
    }

    /*
     * @see android.widget.Checkable#toggle()
     */
    @Override
    public void toggle() {
        isChecked = !isChecked;
        refreshDrawableState();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] states = super.onCreateDrawableState(extraSpace + 1);

        if (isChecked) mergeDrawableStates(states, CHECKED_STATE);

        return states;
    }
}
