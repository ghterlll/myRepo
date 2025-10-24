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

    private static final int AUTO_HIDE_DELAY = 3000; // 3 seconds
    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final float EXPANDED_SIZE = 56f; // dp
    private static final float COLLAPSED_SIZE = 16f; // dp
    private static final float DRAG_THRESHOLD = 10f; // dp

    private Paint buttonPaint;
    private Paint ripplePaint;
    private float currentX;
    private float currentY;
    private float startX;
    private float startY;
    private float dX;
    private float dY;
    private boolean isDragging = false;
    private boolean isExpanded = true;
    private boolean isLongPressed = false;
    private float currentSize;
    private float rippleRadius = 0f;
    private int screenWidth;
    private int screenHeight;

    private Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private Handler longPressHandler = new Handler(Looper.getMainLooper());

    private OnClickListener clickListener;
    private OnStateChangeListener stateChangeListener;

    public interface OnStateChangeListener {
        void onExpanded();
        void onCollapsed();
    }

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

        currentSize = dpToPx(EXPANDED_SIZE);

        // Start auto-hide timer
        scheduleAutoHide();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // Initial position: bottom-right corner
        if (currentX == 0 && currentY == 0) {
            currentX = screenWidth - dpToPx(EXPANDED_SIZE) - dpToPx(16);
            currentY = screenHeight - dpToPx(EXPANDED_SIZE) - dpToPx(16);
            setTranslationX(currentX);
            setTranslationY(currentY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = currentSize / 2f;

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

        // Draw plus icon (only when expanded)
        if (isExpanded) {
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
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                dX = getTranslationX() - event.getRawX();
                dY = getTranslationY() - event.getRawY();
                isDragging = false;
                isLongPressed = false;

                cancelAutoHide();
                scheduleLongPress();

                playPressAnimation();
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaX = Math.abs(event.getRawX() - startX);
                float deltaY = Math.abs(event.getRawY() - startY);

                if (deltaX > dpToPx(DRAG_THRESHOLD) || deltaY > dpToPx(DRAG_THRESHOLD)) {
                    isDragging = true;
                    cancelLongPressTimer();

                    if (isExpanded) {
                        setTranslationX(event.getRawX() + dX);
                        setTranslationY(event.getRawY() + dY);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                cancelLongPressTimer();

                if (isDragging) {
                    snapToEdge();
                    scheduleAutoHide();
                } else if (isLongPressed) {
                    // Long press cancelled (moved away and released)
                    playReleaseAnimation();
                    scheduleAutoHide();
                } else {
                    // Normal click
                    handleClick();
                }

                isDragging = false;
                isLongPressed = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancelLongPressTimer();
                playReleaseAnimation();
                scheduleAutoHide();
                isDragging = false;
                isLongPressed = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleClick() {
        if (!isExpanded) {
            // Collapsed state: expand on first click
            expand();
        } else {
            // Expanded state: trigger click listener
            playClickAnimation();
            if (clickListener != null) {
                postDelayed(() -> clickListener.onClick(this), 200);
            }
        }
    }

    private void snapToEdge() {
        float centerX = getTranslationX() + currentSize / 2;
        boolean snapToLeft = centerX < screenWidth / 2;

        float targetX = snapToLeft ?
            dpToPx(16) - currentSize / 2 :
            screenWidth - dpToPx(16) - currentSize / 2;

        ObjectAnimator animator = ObjectAnimator.ofFloat(
            this, "translationX", getTranslationX(), targetX
        );
        animator.setDuration(300);
        animator.setInterpolator(new OvershootInterpolator());
        animator.start();
    }

    public void collapse() {
        if (!isExpanded) return;

        isExpanded = false;

        float targetSize = dpToPx(COLLAPSED_SIZE);
        ValueAnimator animator = ValueAnimator.ofFloat(currentSize, targetSize);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            currentSize = (float) anim.getAnimatedValue();
            requestLayout();
            invalidate();
        });
        animator.start();

        // Move to edge
        float currentCenterX = getTranslationX() + getWidth() / 2f;
        boolean isOnLeft = currentCenterX < screenWidth / 2;
        float targetX = isOnLeft ? -currentSize * 0.7f : screenWidth - currentSize * 0.3f;

        ObjectAnimator moveAnimator = ObjectAnimator.ofFloat(
            this, "translationX", getTranslationX(), targetX
        );
        moveAnimator.setDuration(300);
        moveAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        moveAnimator.start();

        if (stateChangeListener != null) {
            stateChangeListener.onCollapsed();
        }
    }

    public void expand() {
        if (isExpanded) return;

        isExpanded = true;

        float targetSize = dpToPx(EXPANDED_SIZE);
        ValueAnimator animator = ValueAnimator.ofFloat(currentSize, targetSize);
        animator.setDuration(300);
        animator.setInterpolator(new OvershootInterpolator());
        animator.addUpdateListener(anim -> {
            currentSize = (float) anim.getAnimatedValue();
            requestLayout();
            invalidate();
        });
        animator.start();

        // Move back from edge
        float currentCenterX = getTranslationX() + getWidth() / 2f;
        boolean isOnLeft = currentCenterX < screenWidth / 2;
        float targetX = isOnLeft ? dpToPx(16) : screenWidth - dpToPx(EXPANDED_SIZE) - dpToPx(16);

        ObjectAnimator moveAnimator = ObjectAnimator.ofFloat(
            this, "translationX", getTranslationX(), targetX
        );
        moveAnimator.setDuration(300);
        moveAnimator.setInterpolator(new OvershootInterpolator());
        moveAnimator.start();

        scheduleAutoHide();

        if (stateChangeListener != null) {
            stateChangeListener.onExpanded();
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
        ValueAnimator rippleAnimator = ValueAnimator.ofFloat(0f, currentSize * 1.5f);
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

    private void scheduleAutoHide() {
        cancelAutoHide();
        autoHideHandler.postDelayed(() -> {
            if (isExpanded) {
                collapse();
            }
        }, AUTO_HIDE_DELAY);
    }

    private void cancelAutoHide() {
        autoHideHandler.removeCallbacksAndMessages(null);
    }

    private void scheduleLongPress() {
        longPressHandler.postDelayed(() -> {
            if (!isDragging) {
                isLongPressed = true;
                performHapticFeedback(HAPTIC_FEEDBACK_ENABLED);
            }
        }, LONG_PRESS_TIMEOUT);
    }

    private void cancelLongPressTimer() {
        longPressHandler.removeCallbacksAndMessages(null);
    }

    public void resetAutoHideTimer() {
        if (isExpanded) {
            scheduleAutoHide();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) currentSize;
        setMeasuredDimension(size, size);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        this.clickListener = l;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAutoHide();
        cancelLongPressTimer();
    }
}
