package com.example.pomodoro.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import com.example.pomodoro.R

class PieProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var color = 0
    private var style = FILL
    private val paint = Paint()
    private var progress = 0f

    init {
        if (attrs != null) {
            val styledAttrs = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.PieProgressIndicator,
                defStyleAttr,
                0
            )
            color = styledAttrs.getColor(R.styleable.PieProgressIndicator_pie_color, Color.BLACK)
            style = styledAttrs.getColor(R.styleable.PieProgressIndicator_pie_style, FILL)
            styledAttrs.recycle()
            paint.isAntiAlias = true
        }

        paint.color = color
        paint.style = if (style == FILL) Paint.Style.FILL else Paint.Style.STROKE
        paint.strokeWidth = 5f
    }

    /**
     * Sets progress.
     * @param progress Progress percentage. Must be in [0, 1].
     */
    fun setProgress(progress: Float) {
        if (progress < 0 || progress > 1) {
            throw IllegalArgumentException("Progress is out of bounds. Must be 0..1")
        }

        this.progress = progress
        invalidate()
        requestLayout()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val sweepAngle = progress * 360
        canvas?.drawArc(
            0f, 0f, width.toFloat(), height.toFloat(),
            -90f, sweepAngle, true, paint)
    }

    private companion object {
        private const val FILL = 0
    }
}
