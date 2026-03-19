package com.netspeed.monitor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

/**
 * Generates bitmap icons for the status bar notification.
 * Renders compact speed text onto a small bitmap for dynamic notification icons.
 */
public final class SpeedIconGenerator {

    // Private constructor prevents instantiation
    private SpeedIconGenerator() {}

    // Icon canvas size in pixels — Android downscales for status bar
    private static final int SIZE = 130;

    // Font sizes for value line and unit line
    private static final float VALUE_TEXT_SIZE = 90f;
    private static final float UNIT_TEXT_SIZE = 52f;

    // Gap between value and unit text lines
    private static final float LINE_GAP = 2f;

    // Tighter letter spacing for compact rendering
    private static final float LETTER_SPACING = -0.05f;

    /**
     * Creates a bitmap icon showing the download speed.
     * The icon serves as the notification small icon in the status bar.
     */
    public static Bitmap createSpeedIcon(double downloadSpeed) {
        // Create transparent bitmap canvas
        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmap);

        // Format speed into compact value + unit pair
        String[] formatted = SpeedUtils.formatSpeedCompact(downloadSpeed);
        String value = formatted[0];
        String unit = formatted[1];

        // Configure condensed bold typeface for maximum readability at small sizes
        Typeface typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        float cx = SIZE / 2f;

        // Paint for the large speed value number
        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.WHITE);
        valuePaint.setTypeface(typeface);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(VALUE_TEXT_SIZE);
        valuePaint.setLetterSpacing(LETTER_SPACING);

        // Paint for the smaller unit label
        Paint unitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unitPaint.setColor(Color.WHITE);
        unitPaint.setTypeface(typeface);
        unitPaint.setTextAlign(Paint.Align.CENTER);
        unitPaint.setTextSize(UNIT_TEXT_SIZE);
        unitPaint.setLetterSpacing(LETTER_SPACING);

        // Measure text bounds to center both lines vertically
        Rect valueBounds = new Rect();
        valuePaint.getTextBounds(value, 0, value.length(), valueBounds);
        Rect unitBounds = new Rect();
        unitPaint.getTextBounds(unit, 0, unit.length(), unitBounds);

        // Calculate vertical positions to center text block on canvas
        float totalHeight = valueBounds.height() + LINE_GAP + unitBounds.height();
        float startY = (SIZE - totalHeight) / 2f;

        // Draw value text on the top line
        float valueY = startY + valueBounds.height();
        canvas.drawText(value, cx, valueY, valuePaint);

        // Draw unit text on the bottom line
        float unitY = valueY + LINE_GAP + unitBounds.height();
        canvas.drawText(unit, cx, unitY, unitPaint);

        return bitmap;
    }
}
