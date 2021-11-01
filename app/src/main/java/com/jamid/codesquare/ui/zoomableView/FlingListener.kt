package com.jamid.codesquare.ui.zoomableView

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.navigation.fragment.findNavController
import com.jamid.codesquare.ui.ImageViewFragment

class FlingListener(val fragment: ImageViewFragment): GestureDetector.SimpleOnGestureListener() {
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        fragment.findNavController().navigateUp()
        return super.onFling(e1, e2, velocityX, velocityY)
    }
}