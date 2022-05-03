package com.jamid.codesquare.ui

import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar

class NoSwipeBehavior : BaseTransientBottomBar.Behavior() {
    override fun canSwipeDismissView(child: View): Boolean {
        return false
    }
}