package com.jamid.codesquare.listeners

import android.view.View
import com.jamid.codesquare.data.Image
// something simple
interface ImageClickListener {
    fun onImageClick(view: View, image: Image)

    /**
     * Action on close button overlay click present on the image
     * */
    fun onCloseBtnClick(view: View, image: Image, position: Int)
}