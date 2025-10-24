package com.aura.starter.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.aura.starter.WeightTrendActivity.WeightEntry;

import java.text.SimpleDateFormat;
import java.util.*;

public class TrendChartView extends View {

    private final List<WeightEntry> data = new ArrayList<>();
    private float target = Float.NaN;

    // paints
    private final Paint gridP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridBottomP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textB = new Rect();

    private final SimpleDateFormat df = new SimpleDateFormat("M/d", Locale.getDefault());

    // paddings (right/top/bottom)
    private float padR, padT, padB;

    // gap between left labels and chart area (left label width is computed at runtime)
    private float labelGapPx; // can be 0 or negative
    private float minLabelPadding; // minimum padding from left edge

    public TrendChartView(Context c) { this(c, null); }
    public TrendChartView(Context c, AttributeSet a) { this(c, a, 0); }
    public TrendChartView(Context c, AttributeSet a, int s) {
        super(c, a, s);
        float d = getResources().getDisplayMetrics().density;

        padR = 32 * d;
        padT = 24 * d;
        padB = 40 * d;

        // spacing can be negative to move labels closer (e.g., -2dp)
        labelGapPx = 16f * d;
        minLabelPadding = 6 * d;

        gridP.setColor(0xFFE0E0E0);
        gridP.setStrokeWidth(1 * d);

         // bottom baseline slightly thicker for visual emphasis
        gridBottomP.setColor(0xFFDADADA);
        gridBottomP.setStrokeWidth(1.5f * d);

        lineP.setColor(0xFF54B266);
        lineP.setStrokeWidth(2.5f * d);
        lineP.setStyle(Paint.Style.STROKE);

        fillP.setColor(0x3354B266);
        fillP.setStyle(Paint.Style.FILL);

        pointP.setColor(0xFF54B266);

        textP.setColor(0xFF666666);
        textP.setTextSize(12 * d);

        targetP.setColor(0xFF54B266);
        targetP.setPathEffect(new DashPathEffect(new float[]{8*d, 8*d}, 0));
        targetP.setStrokeWidth(2 * d);
        targetP.setStyle(Paint.Style.STROKE);
    }

    public void setData(List<WeightEntry> entries, float targetKg) {
        data.clear();
        if (entries != null) data.addAll(entries);
        target = targetKg;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (data.isEmpty()) return;

        float w = getWidth(), h = getHeight();
        float T = padT, B = h - padB;
        float R = w - padR;

         // 1) compute y-axis range and labels
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (WeightEntry e : data) { min = Math.min(min, e.kg); max = Math.max(max, e.kg); }
        float range = Math.max(1f, (max - min));
        min -= range * 0.2f; max += range * 0.2f;

        int rows = 5;
        // precompute label strings and measure max width
        float maxLabelW = 0f;
        String[] yLabels = new String[rows + 1];
        for (int i = 0; i <= rows; i++) {
            float val = max - (max - min) * i / rows;
            String label = String.format(Locale.getDefault(), "%.0f", val);
            yLabels[i] = label;
            textP.getTextBounds(label, 0, label.length(), textB);
            maxLabelW = Math.max(maxLabelW, textB.width());
        }

         // 2) dynamic left label area width (max label width + padding)
        float leftLabelArea = maxLabelW + minLabelPadding * 2;

         // actual chart left bound = label area + gap (gap can be negative to bring chart closer)
        float L = leftLabelArea + labelGapPx;

         // 3) draw grid lines and y labels
        for (int i = 0; i <= rows; i++) {
            float y = T + (B - T) * i / rows;
            if (i == rows) {
                 // extend bottom baseline a bit on both ends
                float extend = 6 * getResources().getDisplayMetrics().density;
                c.drawLine(L - extend, y, R + extend, y, gridBottomP);
            } else {
                c.drawLine(L, y, R, y, gridP);
            }

            String label = yLabels[i];
            textP.getTextBounds(label, 0, label.length(), textB);
             // right-align label text to the label area with small padding
            float labelX = leftLabelArea - textB.width() - minLabelPadding;
            c.drawText(label, labelX, y + textB.height()/2f, textP);
        }

         // 4) x-axis date labels (move with chart)
        int n = data.size();
        for (int i = 0; i < n; i++) {
            float x = L + (R - L) * i / Math.max(1, n - 1);
            String d = df.format(data.get(i).date);
            textP.getTextBounds(d,0,d.length(),textB);
            float y = B + textB.height() + 15;
            c.drawText(d, x - textB.width()/2f, y, textP);
        }

         // 5) target dashed line
        if (!Float.isNaN(target)) {
            float ty = T + (max - target) * (B - T) / (max - min);
            c.drawLine(L, ty, R, ty, targetP);
        }

         // 6) line path & fill
        Path path = new Path();
        for (int i = 0; i < n; i++) {
            float x = L + (R - L) * i / Math.max(1, n - 1);
            float y = T + (max - data.get(i).kg) * (B - T) / (max - min);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        Path area = new Path(path);
        area.lineTo(R, B); area.lineTo(L, B); area.close();
        c.drawPath(area, fillP);
        c.drawPath(path, lineP);

         // 7) points + value bubbles; allow slight overflow to keep centered above points
        float d = getResources().getDisplayMetrics().density;
        float r = 4 * d;

        Paint labelBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelBg.setColor(0xFF54B266);
        Paint labelText = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelText.setColor(Color.WHITE);
        labelText.setTextSize(textP.getTextSize());

         // minimal overflow allowance to avoid clamping too hard on edges
        float bubbleOverflow = 14 * d;

        for (int i = 0; i < n; i++) {
            float x = L + (R - L) * i / Math.max(1, n - 1);
            float y = T + (max - data.get(i).kg) * (B - T) / (max - min);
            c.drawCircle(x, y, r, pointP);

            String val = String.format(Locale.getDefault(),"%.1f", data.get(i).kg);
            textP.getTextBounds(val,0,val.length(),textB);
            float rectW = textB.width() + 24;

            float bx = x - rectW / 2f;

            if (bx < 0 - bubbleOverflow) bx = 0 - bubbleOverflow;
            if (bx + rectW > w + bubbleOverflow) bx = w + bubbleOverflow - rectW;

            float by = y - textB.height() - 14;
            RectF rect = new RectF(bx, by - textB.height() - 12, bx + rectW, by + 8);
            c.drawRoundRect(rect, 16, 16, labelBg);
            c.drawText(val, bx + 12, by, labelText);
        }
    }
}
