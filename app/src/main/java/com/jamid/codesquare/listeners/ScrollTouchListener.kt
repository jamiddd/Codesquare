package com.jamid.codesquare.listeners

import android.util.Log
import android.view.MotionEvent

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import kotlin.math.abs

class ScrollTouchListener : OnItemTouchListener {

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                rv.parent.requestDisallowInterceptTouchEvent(true)
            }
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

    }

    companion object {
        private const val TAG = "ScrollTouchListener"
    }

}