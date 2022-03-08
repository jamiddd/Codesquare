package com.jamid.codesquare

import android.graphics.drawable.Animatable
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo

class FrescoImageControllerListener(private val onImageLoaded: ((width: Int, height: Int) -> Unit)? = null): BaseControllerListener<ImageInfo>() {

    var finalWidth = 0
    var finalHeight = 0

    override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
        super.onFinalImageSet(id, imageInfo, animatable)
        if (imageInfo != null) {
            finalWidth = imageInfo.width
            finalHeight = imageInfo.height
            onImageLoaded?.let {
                it(imageInfo.width, imageInfo.height)
            }
        }
    }
}