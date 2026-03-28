package com.netspeed.monitor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

/**
 * Generates bitmap icons for the status bar notification.
 * Renders compact speed text onto a reusable bitmap for dynamic notification icons.
 * Uses static Paint/Bitmap objects to minimize allocations at high update rates.
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

    // Reusable bitmap — cleared and redrawn each call to avoid GC pressure
    private static Bitmap reusableBitmap;
    // Canvas attached to the reusable bitmap
    private static Canvas reusableCanvas;

    // Pre-allocated Paint for the large speed value number
    private static final Paint VALUE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Pre-allocated Paint for the smaller unit label
    private static final Paint UNIT_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Reusable Rect objects for text measurement to avoid per-call allocation
    private static final Rect VALUE_BOUNDS = new Rect();
    private static final Rect UNIT_BOUNDS = new Rect();

    // Static initializer configures paints once at class load time
    static {
        Typeface typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD);

        // Configure value paint — large bold number
        VALUE_PAINT.setColor(Color.WHITE);
        VALUE_PAINT.setTypeface(typeface);
        VALUE_PAINT.setTextAlign(Paint.Align.CENTER);
        VALUE_PAINT.setTextSize(VALUE_TEXT_SIZE);
        VALUE_PAINT.setLetterSpacing(LETTER_SPACING);

        // Configure unit paint — smaller unit label
        UNIT_PAINT.setColor(Color.WHITE);
        UNIT_PAINT.setTypeface(typeface);
        UNIT_PAINT.setTextAlign(Paint.Align.CENTER);
        UNIT_PAINT.setTextSize(UNIT_TEXT_SIZE);
        UNIT_PAINT.setLetterSpacing(LETTER_SPACING);
    }

    /**
     * Creates a bitmap icon showing the download speed.
     * Reuses a single bitmap and pre-allocated paints to stay efficient at 100ms update rates.
     */
    public static Bitmap createSpeedIcon(double downloadSpeed) {
        // Lazily create the reusable bitmap and canvas on first call
        if (reusableBitmap == null) {
            reusableBitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ALPHA_8);
            reusableCanvas = new Canvas(reusableBitmap);
        }

        // Clear previous drawing — erase to transparent
        reusableBitmap.eraseColor(Color.TRANSPARENT);

        // Format speed into compact value + unit pair
        String[] formatted = SpeedUtils.formatSpeedCompact(downloadSpeed);
        String value = formatted[0];
        String unit = formatted[1];

        // Center X coordinate on the canvas
        float cx = SIZE / 2f;

        // Measure text bounds to center both lines vertically
        VALUE_PAINT.getTextBounds(value, 0, value.length(), VALUE_BOUNDS);
        UNIT_PAINT.getTextBounds(unit, 0, unit.length(), UNIT_BOUNDS);

        // Calculate vertical positions to center text block on canvas
        float totalHeight = VALUE_BOUNDS.height() + LINE_GAP + UNIT_BOUNDS.height();
        float startY = (SIZE - totalHeight) / 2f;

        // Draw value text on the top line
        float valueY = startY + VALUE_BOUNDS.height();
        reusableCanvas.drawText(value, cx, valueY, VALUE_PAINT);

        // Draw unit text on the bottom line
        float unitY = valueY + LINE_GAP + UNIT_BOUNDS.height();
        reusableCanvas.drawText(unit, cx, unitY, UNIT_PAINT);

        return reusableBitmap;
    }
}
