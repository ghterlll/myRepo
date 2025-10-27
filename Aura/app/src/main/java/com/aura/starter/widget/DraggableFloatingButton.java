package com.aura.starter.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.aura.starter.R;

public class DraggableFloatingButton extends View {

    private static final float BUTTON_SIZE = 56f; // dp
    private static final float DRAG_THRESHOLD = 10f; // dp
    private static final float MARGIN_FROM_EDGE = 16f; // dp
    private static final float BOTTOM_NAV_HEIGHT = 56f; // dp - bottom navigation bar height
    private static final int SNAP_ANIMATION_DURATION = 300; // ms

    // SharedPreferences keys
    private static final String PREFS_NAME = "FloatingButtonPrefs";
    private static final String KEY_POSITION_X = "position_x";
    private static final String KEY_POSITION_Y = "position_y";
    private static final String KEY_IS_FIRST_TIME = "is_first_time";

    private Paint buttonPaint;
    private Paint ripplePaint;
    private SharedPreferences prefs;

    // Touch tracking
    private float startX;
    private float startY;
    private float dX;
    private float dY;
    private boolean isDragging = false;

    // Screen dimensions
    private int screenWidth;
    private int screenHeight;

    // Animation
    private float rippleRadius = 0f;

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
        // Initialize paints
        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setColor(ContextCompat.getColor(context, R.color.auragreen_primary));
        buttonPaint.setStyle(Paint.Style.FILL);

        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setStyle(Paint.Style.FILL);

        // Get screen dimensions
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        // Initialize SharedPreferences
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Check if this is the first time
        boolean isFirstTime = prefs.getBoolean(KEY_IS_FIRST_TIME, true);

        if (isFirstTime) {
            // First time - set to bottom-right corner
            float initialX = screenWidth - dpToPx(BUTTON_SIZE + MARGIN_FROM_EDGE);
            float initialY = screenHeight - dpToPx(BUTTON_SIZE + MARGIN_FROM_EDGE + BOTTOM_NAV_HEIGHT);
            setTranslationX(initialX);
            setTranslationY(initialY);

            // Save position and mark as no longer first time
            savePosition(initialX, initialY);
            prefs.edit().putBoolean(KEY_IS_FIRST_TIME, false).apply();
        } else {
            // Load saved position
            float savedX = prefs.getFloat(KEY_POSITION_X, -1);
            float savedY = prefs.getFloat(KEY_POSITION_Y, -1);

            if (savedX != -1 && savedY != -1) {
                setTranslationX(savedX);
                setTranslationY(savedY);
            }
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
                // Record initial touch position
                startX = event.getRawX();
                startY = event.getRawY();
                dX = getTranslationX() - event.getRawX();
                dY = getTranslationY() - event.getRawY();
                isDragging = false;
                playPressAnimation();
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaX = Math.abs(event.getRawX() - startX);
                float deltaY = Math.abs(event.getRawY() - startY);
                float totalDelta = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                if (totalDelta > dpToPx(DRAG_THRESHOLD)) {
                    isDragging = true;

                    // Allow free movement in both X and Y directions
                    float newX = event.getRawX() + dX;
                    float newY = event.getRawY() + dY;

                    // Constrain within screen bounds
                    float maxX = screenWidth - dpToPx(BUTTON_SIZE + MARGIN_FROM_EDGE);
                    float minX = dpToPx(MARGIN_FROM_EDGE);
                    // Prevent dragging into bottom navigation bar area
                    float maxY = screenHeight - dpToPx(BUTTON_SIZE + MARGIN_FROM_EDGE + BOTTOM_NAV_HEIGHT);
                    float minY = dpToPx(MARGIN_FROM_EDGE);

                    newX = Math.max(minX, Math.min(maxX, newX));
                    newY = Math.max(minY, Math.min(maxY, newY));

                    setTranslationX(newX);
                    setTranslationY(newY);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isDragging) {
                    // Normal click (no drag)
                    handleClick();
                } else {
                    // Was dragging - snap to nearest edge
                    snapToNearestEdge();
                }

                isDragging = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    snapToNearestEdge();
                } else {
                    playReleaseAnimation();
                }
                isDragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Snap button to the nearest vertical edge (left or right)
     * Keeps the current Y position unchanged
     */
    private void snapToNearestEdge() {
        float currentX = getTranslationX();
        float currentY = getTranslationY();

        // Calculate center of button
        float buttonCenterX = currentX + dpToPx(BUTTON_SIZE) / 2f;
        float screenCenterX = screenWidth / 2f;

        // Determine target X position (left or right edge)
        float targetX;
        if (buttonCenterX < screenCenterX) {
            // Snap to left edge
            targetX = dpToPx(MARGIN_FROM_EDGE);
        } else {
            // Snap to right edge
            targetX = screenWidth - dpToPx(BUTTON_SIZE + MARGIN_FROM_EDGE);
        }

        // Save the new position
        savePosition(targetX, currentY);

        // Animate to target position
        ObjectAnimator animX = ObjectAnimator.ofFloat(this, "translationX", currentX, targetX);
        animX.setDuration(SNAP_ANIMATION_DURATION);
        animX.setInterpolator(new OvershootInterpolator(1.2f));

        // Also play release animation
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0.9f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0.9f, 1.0f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.setInterpolator(new OvershootInterpolator());
        scaleY.setInterpolator(new OvershootInterpolator());

        animSet.playTogether(animX, scaleX, scaleY);
        animSet.start();
    }

    /**
     * Save button position to SharedPreferences
     */
    private void savePosition(float x, float y) {
        prefs.edit()
            .putFloat(KEY_POSITION_X, x)
            .putFloat(KEY_POSITION_Y, y)
            .apply();
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
