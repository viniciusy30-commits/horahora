package com.horacerta.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var showSecondHand: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var accentColor: Int = 0xFF4FD1C5.toInt()
        set(value) {
            field = value
            ringPaint.color = value
            secondPaint.color = value
            hubPaint.color = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF121A30.toInt()
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        color = 0xFF4FD1C5.toInt()
    }
    private val tickMajor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        color = 0xFFFFFFFF.toInt()
    }
    private val tickMinor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(1.2f)
        strokeCap = Paint.Cap.ROUND
        color = 0x66FFFFFF
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = dp(18f)
        color = 0xCCFFFFFF.toInt()
    }
    private val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(6f)
        strokeCap = Paint.Cap.ROUND
        color = 0xFFFFFFFF.toInt()
    }
    private val minutePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(4f)
        strokeCap = Paint.Cap.ROUND
        color = 0xFFE6ECFF.toInt()
    }
    private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(2f)
        strokeCap = Paint.Cap.ROUND
        color = 0xFFFF6B6B.toInt()
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF4FD1C5.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - dp(6f)

        canvas.drawCircle(cx, cy, r, facePaint)
        canvas.drawCircle(cx, cy, r, ringPaint)

        // Marcas dos minutos e das horas
        for (i in 0 until 60) {
            val major = i % 5 == 0
            val len = if (major) dp(12f) else dp(5f)
            val start = cy - r + dp(10f)
            canvas.save()
            canvas.rotate(i * 6f, cx, cy)
            canvas.drawLine(cx, start, cx, start + len, if (major) tickMajor else tickMinor)
            canvas.restore()
        }

        // Números de 1 a 12
        val numR = r - dp(40f)
        val yOffset = -(textPaint.ascent() + textPaint.descent()) / 2f
        for (n in 1..12) {
            val a = Math.toRadians(n * 30.0)
            val x = cx + (sin(a) * numR).toFloat()
            val y = cy - (cos(a) * numR).toFloat() + yOffset
            canvas.drawText(n.toString(), x, y, textPaint)
        }

        val cal = Calendar.getInstance()
        val sec = cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND) / 1000f
        val minute = cal.get(Calendar.MINUTE) + sec / 60f
        val hour = cal.get(Calendar.HOUR) + minute / 60f

        hand(canvas, cx, cy, hour * 30f, r * 0.5f, dp(8f), hourPaint)
        hand(canvas, cx, cy, minute * 6f, r * 0.74f, dp(10f), minutePaint)
        if (showSecondHand) {
            hand(canvas, cx, cy, sec * 6f, r * 0.84f, dp(20f), secondPaint)
        }
        canvas.drawCircle(cx, cy, dp(6f), hubPaint)

        if (showSecondHand) postInvalidateOnAnimation() else postInvalidateDelayed(1000)
    }

    private fun hand(
        canvas: Canvas, cx: Float, cy: Float,
        angle: Float, length: Float, tail: Float, paint: Paint
    ) {
        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.drawLine(cx, cy + tail, cx, cy - length, paint)
        canvas.restore()
    }
}
