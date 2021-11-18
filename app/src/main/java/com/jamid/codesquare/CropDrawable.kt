package com.jamid.codesquare

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable


class CropDrawable(resources: Resources, bitmap: Bitmap?) : BitmapDrawable(resources, bitmap) {

    private val p: Path = Path()
    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        p.rewind()
        p.addCircle(
            (bounds.width() / 2).toFloat(),
            (bounds.height() / 2).toFloat(),
            (bounds.width().coerceAtMost(bounds.height()) / 2).toFloat(),
            Path.Direction.CW
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(p)
    }
}