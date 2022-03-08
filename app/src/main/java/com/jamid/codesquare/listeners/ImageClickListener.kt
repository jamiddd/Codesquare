package com.jamid.codesquare.listeners

import android.view.View
import com.jamid.codesquare.data.Image

interface ImageClickListener {
    fun onImageClick(view: View, image: Image)
    fun onCloseBtnClick(view: View, image: Image, position: Int)
}