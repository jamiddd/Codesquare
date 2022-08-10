package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep// something simple
data class OnBoardingData(
    val content: String,
    val image: Int
): Parcelable
