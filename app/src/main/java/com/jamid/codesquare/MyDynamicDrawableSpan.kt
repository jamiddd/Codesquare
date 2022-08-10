package com.jamid.codesquare

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.style.DynamicDrawableSpan
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * StyleSpan for putting image into text. May need later
 * @param context Context for drawable
 * @param resourceId Image resource for putting inside text
 * */// something simple
internal class MyDynamicDrawableSpan(val context: Context, @DrawableRes val resourceId: Int) : DynamicDrawableSpan() {

    override fun getDrawable(): Drawable {
        val drawable = ContextCompat.getDrawable(context, resourceId)!!
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        return drawable
    }

}