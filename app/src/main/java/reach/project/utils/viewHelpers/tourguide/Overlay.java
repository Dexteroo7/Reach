package reach.project.utils.viewHelpers.tourguide;

import android.graphics.Color;
import android.view.View;
import android.view.animation.Animation;

/**
 * {@link Overlay} shows a tinted background to cover up the rest of the screen. A 'hole' will be made on this overlay to let users obtain focus on the targeted element.
 */
public class Overlay {
    public int mBackgroundColor;
    public boolean mDisableClick;
    public Style mStyle;
    public Animation mEnterAnimation, mExitAnimation;
    public View.OnClickListener mOnClickListener;
    public int mHoleRadius = NOT_SET;
    public final static int NOT_SET = -1;

    public enum Style {
        Circle, Rectangle
    }
    public Overlay() {
        this(true, Color.parseColor("#55000000"), Style.Circle);
    }

    public Overlay(boolean disableClick, int backgroundColor, Style style) {
        mDisableClick = disableClick;
        mBackgroundColor = backgroundColor;
        mStyle = style;
    }

    /**
     * Set background color
     * @param backgroundColor
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setBackgroundColor(int backgroundColor){
        mBackgroundColor = backgroundColor;
        return this;
    }

    /**
     * Set to true if you want to block all user input to pass through this overlay, set to false if you want to allow user input under the overlay
     * @param yes_no
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay disableClick(boolean yes_no){
        mDisableClick = yes_no;
        return this;
    }

    public Overlay setStyle(Style style){
        mStyle = style;
        return this;
    }

    /**
     * Set enter animation
     * @param enterAnimation
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setEnterAnimation(Animation enterAnimation){
        mEnterAnimation = enterAnimation;
        return this;
    }
    /**
     * Set exit animation
     * @param exitAnimation
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setExitAnimation(Animation exitAnimation){
        mExitAnimation = exitAnimation;
        return this;
    }

    /**
     * Set {@link Overlay#mOnClickListener} for the {@link Overlay}
     * @param onClickListener
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setOnClickListener(View.OnClickListener onClickListener){
        mOnClickListener=onClickListener;
        return this;
    }

    /**
     * This method sets the hole's radius.
     * If this is not set, the size of view hole fill follow the max(view.width, view.height)
     * If this is set, it will take precedence
     * It only has effect when {@link Overlay.Style#Circle} is chosen
     * @param holeRadius the radius of the view hole, setting 0 will make the hole disappear, in pixels
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setHoleRadius(int holeRadius) {
        mHoleRadius = holeRadius;
        return this;
    }
}
