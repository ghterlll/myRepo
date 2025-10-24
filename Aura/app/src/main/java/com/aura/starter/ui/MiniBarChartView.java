package com.aura.starter.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MiniBarChartView extends View {
    private int[] data = new int[]{0,0,0,0,0,0,0};
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);

    public MiniBarChartView(Context c) { super(c); init(); }
    public MiniBarChartView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        bar.setColor(0xFF4CAF50);
        axis.setColor(0xFF888888);
        axis.setStrokeWidth(2f);
    }

    public void setData(int[] d) {
        if (d != null && d.length == 7) {
            data = d;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int padding = 16;
        int chartW = w - padding*2;
        int chartH = h - padding*2;
        int max = 1;
        for (int v : data) if (v > max) max = v;

        canvas.drawLine(padding, h - padding, w - padding, h - padding, axis);

        int barWidth = chartW / (data.length * 2);
        for (int i=0;i<data.length;i++) {
            float x = padding + i * (barWidth * 2) + barWidth * 0.5f;
            float bh = (data[i] * 1f / max) * (chartH - 10);
            canvas.drawRect(x, h - padding - bh, x + barWidth, h - padding, bar);
        }
    }
}
