package com.xiu.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.chibde.BaseVisualizer;
import com.xiu.xtmusic.R;

/**
 * Created by xiu on 2018/1/25.
 */

public class CustomVisualizer extends BaseVisualizer {

    private Paint middleLine;
    private float density;
    private int gap;

    public CustomVisualizer(Context context) {
        super(context);
    }

    public CustomVisualizer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVisualizer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        density = 10;
        gap = 4;
        middleLine = new Paint();
        middleLine.setColor(Color.TRANSPARENT);
    }

    /**
     * Sets the density to the Bar visualizer i.e the number of bars
     * to be displayed. Density can vary from 10 to 256.
     * by default the value is set to 50.
     *
     * @param density density of the bar visualizer
     */
    public void setDensity(float density) {
        if (this.density > 180) {
            this.middleLine.setStrokeWidth(1);
            this.gap = 1;
        } else {
            this.gap = 4;
        }
        this.density = density;
        if (density > 256) {
            this.density = 250;
            this.gap = 0;
        } else if (density <= 10) {
            this.density = 10;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bytes != null) {
            float barWidth = getWidth() / density;
            float div = bytes.length / density;
            canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, middleLine);
            paint.setStrokeWidth(barWidth - gap);

            for (int i = 0; i < density; i++) {
                int x = (int) Math.ceil(i * div);
                int top = canvas.getHeight() / 2
                        + (128 - Math.abs(bytes[x]))
                        * (canvas.getHeight() / 2) / 128;

                int bottom = canvas.getHeight() / 2
                        - (128 - Math.abs(bytes[x]))
                        * (canvas.getHeight() / 2) / 128;

                canvas.drawLine(i * barWidth, bottom, i * barWidth, getHeight() / 2, paint);
                canvas.drawLine(i * barWidth, top, i * barWidth, getHeight() / 2, paint);
            }
            super.onDraw(canvas);
        }
    }
}
