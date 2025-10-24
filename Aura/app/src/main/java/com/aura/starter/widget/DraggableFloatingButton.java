package com.aura.starter.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.aura.starter.R;

public class DraggableFloatingButton extends View {

    private static final float BUTTON_SIZE = 56f; // dp
    private static final float DRAG_THRESHOLD = 10f; // dp
    private static final float MARGIN_FROM_EDGE = 16f; // dp

    private Paint buttonPaint;
    private Paint ripplePaint;
    private float startY;
    private float dY;
    private boolean isDragging = false;
    private float rippleRadius = 0f;
    private int screenHeight;

    private OnClickListener clickListener;

    public DraggableFloatingButton(Context context) {
        super(context);
        init(context);
    }

    public DraggableFloatingButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setColor(ContextCompat.getColor(context, R.color.auragreen_primary));
        buttonPaint.setStyle(Paint.Style.FILL);

        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenHeight = h;

        // Initial position: right-bottom corner, above bottom navigation
        if (getTranslationY() == 0) {
            // Position at bottom-right, accounting for bottom nav (~56dp) + margin
            float initialY = screenHeight - dpToPx(BUTTON_SIZE + MARGIN_FROM_EDGE + 56);
            setTranslationY(initialY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = dpToPx(BUTTON_SIZE) / 2f;

        // Draw ripple effect
        if (rippleRadius > 0) {
            RadialGradient gradient = new RadialGradient(
                centerX, centerY, rippleRadius,
                new int[]{0x80FFFFFF, 0x00FFFFFF},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
            );
            ripplePaint.setShader(gradient);
            canvas.drawCircle(centerX, centerY, rippleRadius, ripplePaint);
        }

        // Draw main button
        canvas.drawCircle(centerX, centerY, radius, buttonPaint);

        // Draw plus icon
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(0xFFFFFFFF);
        iconPaint.setStrokeWidth(dpToPx(3));
        iconPaint.setStrokeCap(Paint.Cap.ROUND);

        float iconSize = radius * 0.5f;
        // Horizontal line
        canvas.drawLine(
            centerX - iconSize, centerY,
            centerX + iconSize, centerY,
            iconPaint
        );
        // Vertical line
        canvas.drawLine(
            centerX, centerY - iconSize,
            centerX, centerY + iconSize,
            iconPaint
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getRawY();
                dY = getTranslationY() - event.getRawY();
                isDragging = false;
                playPressAnimation();
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaY = Math.abs(event.getRawY() - startY);

                if (deltaY > dpToPx(DRAG_THRESHOLD)) {
                    isDragging = true;

                    // Only allow vertical movement
                    float newY = event.getRawY() + dY;
                    // Constrain within screen bounds
                    float maxY = screenHeight - dpToPx(BUTTON_SIZE + MARGIN_FROM_EDGE);
                    float minY = dpToPx(MARGIN_FROM_EDGE);
                    newY = Math.max(minY, Math.min(maxY, newY));

                    setTranslationY(newY);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isDragging) {
                    // Normal click (no drag)
                    handleClick();
                } else {
                    // Just dragged, release animation only
                    playReleaseAnimation();
                }

                isDragging = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                playReleaseAnimation();
                isDragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleClick() {
        playClickAnimation();
        if (clickListener != null) {
            postDelayed(() -> clickListener.onClick(this), 200);
        }
    }

    private void playPressAnimation() {
        animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    private void playReleaseAnimation() {
        animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .setInterpolator(new OvershootInterpolator())
            .start();
    }

    private void playClickAnimation() {
        // Fingerprint-like ripple animation
        ValueAnimator rippleAnimator = ValueAnimator.ofFloat(0f, dpToPx(BUTTON_SIZE) * 1.5f);
        rippleAnimator.setDuration(400);
        rippleAnimator.addUpdateListener(anim -> {
            rippleRadius = (float) anim.getAnimatedValue();
            invalidate();
        });
        rippleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rippleRadius = 0f;
                invalidate();
            }
        });

        AnimatorSet scaleSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1.0f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1.0f, 1.1f, 1.0f);
        scaleSet.playTogether(scaleX, scaleY);
        scaleSet.setDuration(400);
        scaleSet.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet finalSet = new AnimatorSet();
        finalSet.playTogether(rippleAnimator, scaleSet);
        finalSet.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) dpToPx(BUTTON_SIZE);
        setMeasuredDimension(size, size);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        this.clickListener = l;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
