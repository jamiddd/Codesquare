package com.jamid.codesquare.data

import android.graphics.Color
import com.jamid.codesquare.R

data class ToolbarAdjustment(
    val shouldShowTitle: Boolean = true,
    val titleTextColor: Int = R.color.black,
    val isTitleCentered: Boolean = true
) {
}