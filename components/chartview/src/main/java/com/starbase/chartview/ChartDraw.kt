package com.starbase.chartview

import android.graphics.Canvas

interface ChartDraw {
    var isVisible: Boolean
    fun draw(canvas: Canvas)
}
