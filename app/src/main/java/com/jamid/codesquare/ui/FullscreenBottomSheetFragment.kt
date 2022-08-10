package com.jamid.codesquare.ui

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.codesquare.getBottomSheetBehavior
import com.jamid.codesquare.getFrameLayout
import com.jamid.codesquare.getStatusBarHeight
import com.jamid.codesquare.getWindowHeight
// something simple
abstract class FullscreenBottomSheetFragment(@LayoutRes layout: Int? = null): RoundedBottomSheetDialogFragment(layout) {

    fun setFullHeight() {
        val windowHeight = getWindowHeight()
        val frame = getFrameLayout()
        frame?.updateLayoutParams<ViewGroup.LayoutParams> {
            height = windowHeight - getStatusBarHeight()
        }
        val behavior = getBottomSheetBehavior()
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun setDraggable(isDraggable: Boolean) {
        val behavior = getBottomSheetBehavior()
        behavior?.isDraggable = isDraggable
    }

}