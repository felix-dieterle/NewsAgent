package com.newsagent.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup

/**
 * Small circular indicator showing API rate limit status
 * Green: < 70% usage
 * Yellow: 70-99% usage
 * Red: 100% usage (limit reached)
 */
class RateLimitIndicatorView(context: Context) : View(context) {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private var indicatorColor = Color.GREEN
    
    companion object {
        private const val INDICATOR_SIZE_DP = 12
        const val YELLOW_THRESHOLD = 0.7f // 70%
    }
    
    init {
        // Convert DP to pixels for consistent size across devices
        val density = context.resources.displayMetrics.density
        val sizePixels = (INDICATOR_SIZE_DP * density).toInt()
        layoutParams = ViewGroup.LayoutParams(sizePixels, sizePixels)
    }
    
    /**
     * Update indicator color based on usage percentage
     * @param usagePercent value from 0.0 to 1.0 (0% to 100%)
     */
    fun setUsagePercent(usagePercent: Float) {
        indicatorColor = when {
            usagePercent >= 1.0f -> Color.RED
            usagePercent >= YELLOW_THRESHOLD -> Color.YELLOW
            else -> Color.GREEN
        }
        invalidate()
    }
    
    /**
     * Set color directly
     */
    fun setColor(color: Int) {
        indicatorColor = color
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = indicatorColor
        val radius = width / 2f
        canvas.drawCircle(radius, radius, radius, paint)
    }
}
