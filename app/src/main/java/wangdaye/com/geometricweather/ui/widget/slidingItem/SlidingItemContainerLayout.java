package wangdaye.com.geometricweather.ui.widget.slidingItem;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.widget.ImageViewCompat;

import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.utils.DisplayUtils;
import wangdaye.com.geometricweather.utils.manager.ThemeManager;

public class SlidingItemContainerLayout extends FrameLayout {

    private final AppCompatImageView icon;
    private @Nullable View child;

    private float swipeX;  // total swipe distance: + start, - end

    private @DrawableRes int iconResStart;
    private @DrawableRes int iconResEnd;

    private @ColorInt int backgroundColorStart;
    private @ColorInt int backgroundColorEnd;

    private boolean updateFlag;

    public SlidingItemContainerLayout(Context context) {
        this(context, null);
    }

    public SlidingItemContainerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingItemContainerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int iconSize = (int) DisplayUtils.dpToPx(context, 56);
        int iconPadding = (int) DisplayUtils.dpToPx(context, 16);

        this.icon = new AppCompatImageView(context);
        icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(Color.WHITE));
        addView(icon, new LayoutParams(iconSize, iconSize, Gravity.CENTER_VERTICAL));

        setBackgroundColor(ThemeManager.getInstance(context).getRootColor(context));

        this.child = null;

        this.swipeX = 0;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingItemContainerLayout, defStyleAttr, 0);
        this.iconResStart = a.getResourceId(R.styleable.SlidingItemContainerLayout_iconResStart, 0);
        this.iconResEnd = a.getResourceId(R.styleable.SlidingItemContainerLayout_iconResEnd, 0);
        this.backgroundColorStart = a.getColor(R.styleable.SlidingItemContainerLayout_backgroundColorStart, Color.DKGRAY);
        this.backgroundColorEnd = a.getColor(R.styleable.SlidingItemContainerLayout_backgroundColorEnd, Color.DKGRAY);
        a.recycle();

        this.updateFlag = true;
    }

    public void swipe(float totalX) {
        if (swipeX == totalX) {
            return;
        }
        if (child == null) {
            for (int i = 0; i < getChildCount(); i ++) {
                View v = getChildAt(i);
                if (v != icon) {
                    child = v;
                    break;
                }
            }
        }
        if (child == null) {
            return;
        }

        child.setTranslationX(totalX);

        float progress = Math.abs(1.f * totalX / getMeasuredWidth());
        progress = (float)(1.0f - Math.pow((1.0f - progress), 4));

        if (totalX != 0) { // need to draw background and sliding icon.
            if (totalX * swipeX <= 0 || updateFlag) { // need to set background and sliding icon.
                updateFlag = false;
                if (DisplayUtils.isRtl(getContext())) {
                    icon.setImageResource(totalX < 0 ? iconResStart : iconResEnd);
                    setBackgroundColor(totalX < 0 ? backgroundColorStart : backgroundColorEnd);
                } else {
                    icon.setImageResource(totalX > 0 ? iconResStart : iconResEnd);
                    setBackgroundColor(totalX > 0 ? backgroundColorStart : backgroundColorEnd);
                }
            }
            if (totalX > 0) {
                icon.setTranslationX((float) (
                        0.5 * -icon.getMeasuredWidth() + 0.75 * icon.getMeasuredWidth() * progress
                ));
            } else { // totalX < 0.
                icon.setTranslationX((float) (
                        getMeasuredWidth() - 0.5 * icon.getMeasuredWidth() - 0.75 * icon.getMeasuredWidth() * progress
                ));
            }
        } else {
            setBackgroundColor(ThemeManager.getInstance(getContext()).getRootColor(getContext()));
        }

        swipeX = totalX;
    }

    public int getIconResStart() {
        return iconResStart;
    }

    public void setIconResStart(int iconResStart) {
        this.iconResStart = iconResStart;
        this.updateFlag = true;
    }

    public int getIconResEnd() {
        return iconResEnd;
    }

    public void setIconResEnd(int iconResEnd) {
        this.iconResEnd = iconResEnd;
        this.updateFlag = true;
    }

    public int getBackgroundColorStart() {
        return backgroundColorStart;
    }

    public void setBackgroundColorStart(int backgroundColorStart) {
        this.backgroundColorStart = backgroundColorStart;
        this.updateFlag = true;
    }

    public int getBackgroundColorEnd() {
        return backgroundColorEnd;
    }

    public void setBackgroundColorEnd(int backgroundColorEnd) {
        this.backgroundColorEnd = backgroundColorEnd;
        this.updateFlag = true;
    }
}
