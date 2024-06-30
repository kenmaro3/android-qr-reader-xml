package com.example.qrcodescanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint().apply {
        color = context.getColor(R.color.design_default_color_primary)
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    var qrCodeBounds: android.graphics.Rect? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        qrCodeBounds?.let {
            canvas.drawRect(it, paint)
        }
    }
}
