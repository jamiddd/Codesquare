package com.jamid.codesquare.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.R

class SnackBarAwareBehavior(context: Context, attributeSet: AttributeSet)
    : CoordinatorLayout.Behavior<View>(context, attributeSet){

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        val distanceY = getViewOffsetForSnackbar(parent, child)
        child.translationY = -distanceY
        return true
    }

    private fun getViewOffsetForSnackbar(parent: CoordinatorLayout, view: View): Float{
        var maxOffset = 0f
        val dependencies = parent.getDependencies(view)

        dependencies.forEach { dependency ->
            if (dependency is Snackbar.SnackbarLayout && parent.doViewsOverlap(view, dependency)){
                maxOffset =
                    maxOffset.coerceAtLeast((dependency.translationY - dependency.height - view.context.resources.getDimension(
                        R.dimen.generic_len)) * -1)
            }
        }

        return maxOffset
    }
}