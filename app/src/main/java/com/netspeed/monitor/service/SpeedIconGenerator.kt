package com.netspeed.monitor.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.graphics.createBitmap

object SpeedIconGenerator {

    // ====================================================
    // ALL CONSTANTS - ADJUST FROM HERE
    // ====================================================

    // Icon canvas size in pixels — larger canvas = more detail before Android downscales
    private const val SIZE = 130

    // Font size for speed value (top line: "1.2", "23", etc.)
    private const val VALUE_TEXT_SIZE = 90f

    // Font size for unit label (bottom line: "KB/s", "MB/s", etc.)
    private const val UNIT_TEXT_SIZE = 52f

    // Gap between value and unit lines (pixels)
    private const val LINE_GAP = 2f

    // Letter spacing (negative = tighter)
    private const val LETTER_SPACING = -0.05f

    // Text color
    private const val TEXT_COLOR = Color.WHITE

    // Font family
    private const val FONT_FAMILY = "sans-serif-condensed"

    // Font style: Typeface.BOLD, Typeface.NORMAL, Typeface.ITALIC, Typeface.BOLD_ITALIC
    private val FONT_STYLE = Typeface.BOLD

    // ====================================================

    fun createDualSpeedIcon(
        downloadSpeed: Double,
        @Suppress("UNUSED_PARAMETER") uploadSpeed: Double
    ): Bitmap {
        val bitmap = createBitmap(SIZE, SIZE)
        val canvas = Canvas(bitmap)

        val (value, unit) = formatCompact(downloadSpeed)

        val typeface = Typeface.create(FONT_FAMILY, FONT_STYLE)
        val cx = SIZE / 2f

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_COLOR
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = VALUE_TEXT_SIZE
            letterSpacing = LETTER_SPACING
        }

        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_COLOR
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = UNIT_TEXT_SIZE
            letterSpacing = LETTER_SPACING
        }

        val valueBounds = Rect()
        valuePaint.getTextBounds(value, 0, value.length, valueBounds)

        val unitBounds = Rect()
        unitPaint.getTextBounds(unit, 0, unit.length, unitBounds)

        val totalHeight = valueBounds.height() + LINE_GAP + unitBounds.height()
        val startY = (SIZE - totalHeight) / 2f

        val valueY = startY + valueBounds.height()
        canvas.drawText(value, cx, valueY, valuePaint)

        val unitY = valueY + LINE_GAP + unitBounds.height()
        canvas.drawText(unit, cx, unitY, unitPaint)

        return bitmap
    }

    fun createSpeedIcon(downloadSpeed: Double): Bitmap =
        createDualSpeedIcon(downloadSpeed, 0.0)

    private fun formatCompact(bytesPerSecond: Double): Pair<String, String> {
        return when {
            // 1 GB+ -> "1", "2", etc.
            bytesPerSecond >= 1_073_741_824 ->
                Pair(String.format("%.0f", bytesPerSecond / 1_073_741_824), "GB/s")

            // 100 MB+ -> "100", "234", etc.
            bytesPerSecond >= 104_857_600 ->
                Pair(String.format("%.0f", bytesPerSecond / 1_048_576), "MB/s")

            // 10 MB+ -> "10", "56", etc.
            bytesPerSecond >= 10_485_760 ->
                Pair(String.format("%.0f", bytesPerSecond / 1_048_576), "MB/s")

            // 1 MB+ -> "1.2", "5.8", etc.
            bytesPerSecond >= 1_048_576 ->
                Pair(String.format("%.1f", bytesPerSecond / 1_048_576), "MB/s")

            // 100 KB+ -> "100", "456", etc.
            bytesPerSecond >= 102_400 ->
                Pair(String.format("%.0f", bytesPerSecond / 1024), "KB/s")

            // 1 KB+ -> "1.2", "45.6", etc.
            bytesPerSecond >= 1024 ->
                Pair(String.format("%.1f", bytesPerSecond / 1024), "KB/s")

            // Less than 1 KB -> "512", "64", etc.
            bytesPerSecond > 0 ->
                Pair(String.format("%.0f", bytesPerSecond), "B/s")

            // Zero
            else -> Pair("0", "B/s")
        }
    }
}