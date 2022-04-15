package com.jamid.codesquare.data

import androidx.annotation.Keep
import com.jamid.codesquare.R

@Keep
data class ToolbarAdjustment(
    val shouldShowTitle: Boolean = true,
    val titleTextColor: Int = R.color.black,
    val isTitleCentered: Boolean = true,
    val shouldShowSubTitle: Boolean = false
)