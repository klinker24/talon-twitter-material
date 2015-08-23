package com.klinker.android.twitter_l.manipulations;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.disklrucache.Util;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

public class NavBarOverlayLayout extends LinearLayout {

    // some default constants for initializing the ActionButton
    private Drawable background;
    protected LinearLayout content;

    private View dim;

    // set up default values
    private int distanceFromTop;
    private int distanceFromLeft;
    protected int width;
    protected int height;
    private int screenWidth;
    private int screenHeight;
    private boolean isShowing = false;
    private ViewGroup parent = null;

    public NavBarOverlayLayout(Context context) {
        super(context);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        screenHeight = size.y;
        screenWidth = size.x;

        background = new ColorDrawable(Color.BLACK);
        setBackground(background);

        setOrientation(VERTICAL);

        distanceFromTop = screenHeight;
        distanceFromLeft = 0;
        width = screenWidth;
        height = Utils.hasNavBar(context) ? Utils.getNavBarHeight(context) : 0;
    }

    /**
     * Tells whether or not the button is currently showing on the screen.
     *
     * @return true if ActionButton is showing, false otherwise
     */
    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Animates the ActionButton onto the screen so that the user may interact.
     * Animation occurs from the bottom of the screen, moving up until it reaches the
     * appropriate distance from the bottom.
     */
    public void show() {
        final Activity activity = (Activity) getContext();

        // set the correct width and height for ActionButton
        ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        this.setLayoutParams(params);

        if (parent == null) {
            // get the current content FrameLayout and add ActionButton to the top
            parent = (FrameLayout) activity.findViewById(android.R.id.content);
        }


        try {
            parent.addView(this);
        } catch (Exception e) {

        }

        setTranslationY(distanceFromTop);
        setTranslationX(distanceFromLeft);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
