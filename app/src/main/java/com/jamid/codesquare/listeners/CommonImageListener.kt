package com.jamid.codesquare.listeners

import android.graphics.drawable.Animatable
import android.view.View
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import com.jamid.codesquare.hide

class CommonImageListener(val progressBar: View? = null): BaseControllerListener<ImageInfo>()  {

    var finalWidth = 0
    var finalHeight = 0

    override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
        super.onFinalImageSet(id, imageInfo, animatable)

        progressBar?.hide()

        if (imageInfo != null) {
            finalWidth = imageInfo.width
            finalHeight = imageInfo.height
        }

    }

    override fun onFailure(id: String?, throwable: Throwable?) {
        super.onFailure(id, throwable)

        progressBar?.hide()

    }

}