package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * "Use [com.jamid.codesquare.BaseBottomFragment] instead"
 *
 * */// something simple
@Deprecated("")
open class RoundedBottomSheetDialogFragment(@LayoutRes val layout: Int? = null) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (layout == null) {
            super.onCreateView(inflater, container, savedInstanceState)
        } else {
            inflater.inflate(layout, container, false)
        }
    }

    open fun setScrimVisibility(isVisible: Boolean) {
        val dialog = dialog
        if (dialog != null) {
            val window = dialog.window
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

}