package com.netspeed.monitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom View that draws an arc-style speed gauge with centered speed text.
 * Replaces the Compose SpeedGaugeCard with zero-dependency native canvas drawing.
 */
public class SpeedGaugeView extends View {

    // Arc configuration constants
    private static final float START_ANGLE = 135f;     // Start from bottom-left
    private static final float SWEEP_ANGLE = 270f;     // Sweep 270 degrees clockwise
    private static final double MAX_SPEED_BPS = 100.0 * 1_048_576; // 100 MB/s gauge maximum
    private static final float STROKE_WIDTH_DP = 10f;  // Arc stroke width in DP

    // Current speed in bytes per second
    private double speed = 0;
    // Arc and text color (set per-gauge: green for download, orange for upload)
    private int arcColor = 0xFF00E676;
    // Label displayed below the gauge arc
    private String label = "Download";
    // Arrow symbol displayed above speed text (↓ or ↑)
    private String arrow = "\u2193";

    // Theme-aware colors resolved from resources for dark mode support
    private int colorValueText;
    private int colorLabelText;
    private int colorZeroUnit;

    // Reusable paint objects for efficient drawing
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint unitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Reusable rectangle for arc bounds
    private final RectF arcRect = new RectF();

    public SpeedGaugeView(Context context) {
        super(context);
        initPaints();
    }

    public SpeedGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public SpeedGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    /** Initializes paint objects with default styles for all drawing operations. */
    private void initPaints() {
        float density = getResources().getDisplayMetrics().density;

        // Resolve theme-aware colors from resources (auto-switches for dark mode)
        colorValueText = getContext().getColor(R.color.text_primary);
        colorLabelText = getContext().getColor(R.color.text_secondary);
        colorZeroUnit = getContext().getColor(R.color.text_muted);

        // Track paint: dim arc background showing full gauge range
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        // Arc paint: bright colored arc indicating current speed
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Typeface for all text elements
        Typeface tf = Typeface.create("sans-serif", Typeface.BOLD);

        // Speed value: large bold text in gauge center
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(tf);
        valuePaint.setTextSize(28 * density);

        // Unit label: smaller text below speed value
        unitPaint.setTextAlign(Paint.Align.CENTER);
        unitPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        unitPaint.setTextSize(11 * density);

        // Gauge label: medium text below the arc
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        labelPaint.setTextSize(13 * density);
        // Use theme-aware secondary text color for gauge label
        labelPaint.setColor(colorLabelText);

        // Arrow symbol: colored arrow above speed value
        arrowPaint.setTextAlign(Paint.Align.CENTER);
        arrowPaint.setTypeface(tf);
        arrowPaint.setTextSize(16 * density);
    }

    /** Sets the gauge's arc color (green for download, orange for upload). */
    public void setArcColor(int color) {
        arcColor = color;
        invalidate();
    }

    /** Sets the label displayed below the gauge ("Download" or "Upload"). */
    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

    /** Sets the arrow symbol displayed above speed value ("↓" or "↑"). */
    public void setArrow(String arrow) {
        this.arrow = arrow;
        invalidate();
    }

    /** Updates the displayed speed; triggers a redraw of the gauge. */
    public void setSpeed(double bytesPerSecond) {
        this.speed = bytesPerSecond;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Width is determined by parent layout (e.g., weight in LinearLayout)
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Height is 1.15x width to accommodate arc plus label below
        int height = (int) (width * 1.15f);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float strokePx = STROKE_WIDTH_DP * density;
        float w = getWidth();
        float h = getHeight();

        // Calculate arc bounds: square region centered horizontally with padding
        float arcSize = Math.min(w, h * 0.8f) - strokePx * 2 - 8 * density;
        float cx = w / 2f;
        float arcTop = strokePx + 4 * density;
        float left = cx - arcSize / 2f;
        float right = cx + arcSize / 2f;
        float bottom = arcTop + arcSize;
        arcRect.set(left, arcTop, right, bottom);

        // Center point of the arc for positioning text
        float arcCenterY = (arcTop + bottom) / 2f;

        // Layer 1: background track arc (dim, full 270° sweep)
        trackPaint.setStrokeWidth(strokePx);
        trackPaint.setColor((arcColor & 0x00FFFFFF) | 0x20000000); // 12% opacity
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE, false, trackPaint);

        // Layer 2: speed arc (colored, proportional to current speed)
        float fraction = (float) Math.min(Math.max(speed / MAX_SPEED_BPS, 0.0), 1.0);
        if (fraction > 0.001f) {
            arcPaint.setStrokeWidth(strokePx);
            arcPaint.setColor(arcColor);
            canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE * fraction, false, arcPaint);
        }

        // Format speed into value + unit strings
        String[] formatted = SpeedUtils.formatSpeedCompact(speed);
        String valueText = formatted[0];
        String unitText = formatted[1];

        // Draw arrow symbol above the speed value
        arrowPaint.setColor(arcColor);
        canvas.drawText(arrow, cx, arcCenterY - 18 * density, arrowPaint);

        // Draw speed value number at the arc center with theme-aware color
        valuePaint.setColor(colorValueText);
        canvas.drawText(valueText, cx, arcCenterY + 10 * density, valuePaint);

        // Draw unit label below the speed value (colored when active, muted when zero)
        unitPaint.setColor(speed > 0 ? arcColor : colorZeroUnit);
        canvas.drawText(unitText, cx, arcCenterY + 24 * density, unitPaint);

        // Draw gauge label below the arc
        canvas.drawText(label, cx, bottom + 20 * density, labelPaint);
    }
}
