package com.jamid.codesquare.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnBoardingData(
    val content: String,
    val image: Int
): Parcelable
