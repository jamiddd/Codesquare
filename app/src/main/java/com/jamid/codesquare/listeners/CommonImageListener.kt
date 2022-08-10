package com.jamid.codesquare.listeners

import android.graphics.drawable.Animatable
import android.view.View
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import com.jamid.codesquare.hide
// something simple
class CommonImageListener(val progressBar: View? = null, val onComplete: ((w: Int, h:Int, err: Throwable?) -> Unit)? = null): BaseControllerListener<ImageInfo>()  {

    var finalWidth = 0
    var finalHeight = 0

    override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
        super.onFinalImageSet(id, imageInfo, animatable)
        progressBar?.hide()
        if (imageInfo != null) {
            finalWidth = imageInfo.width
            finalHeight = imageInfo.height

            onComplete?.let { it(finalWidth, finalHeight, null) }
        }

    }

    override fun onFailure(id: String?, throwable: Throwable?) {
        super.onFailure(id, throwable)
        progressBar?.hide()
        onComplete?.let { it(0, 0, throwable) }
    }

}