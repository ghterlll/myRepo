package com.aura.starter.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Custom ViewPager2 wrapper that supports wrap_content height.
 * Measures the current child's height and sets ViewPager2 height accordingly.
 */
public class WrapContentViewPager2 extends FrameLayout {
    private final ViewPager2 viewPager2;

    public WrapContentViewPager2(@NonNull Context context) {
        this(context, null);
    }

    public WrapContentViewPager2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        viewPager2 = new ViewPager2(context);
        viewPager2.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        addView(viewPager2);
    }

    public ViewPager2 getViewPager2() {
        return viewPager2;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(heightMeasureSpec);

        // If height is wrap_content or unspecified, measure based on child height
        if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            int height = 0;
            for (int i = 0; i < viewPager2.getChildCount(); i++) {
                View child = viewPager2.getChildAt(i);
                if (child != null) {
                    child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                    int childHeight = child.getMeasuredHeight();
                    if (childHeight > height) {
                        height = childHeight;
                    }
                }
            }

            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
