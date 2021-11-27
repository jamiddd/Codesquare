package com.jamid.codesquare.listeners

import android.util.Log
import android.view.MotionEvent

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import kotlin.math.abs

class ScrollTouchListener() : OnItemTouchListener {

    private var dir: Int = 1
    var recyclerView: RecyclerView? = null

    private var pastY = 0f

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val action = e.action
        recyclerView = rv

        if (abs(e.y - pastY) > 50f) {
            pastY = e.y
            // scrolling vertically
            rv.parent.requestDisallowInterceptTouchEvent(false)

            return true
        } else {
            pastY = e.y
            return if (rv.canScrollHorizontally(dir)) {
                when (action) {
                    MotionEvent.ACTION_MOVE -> rv.parent
                        .requestDisallowInterceptTouchEvent(true)
                }
                false
            } else {
                when (action) {
                    MotionEvent.ACTION_MOVE -> rv.parent
                        .requestDisallowInterceptTouchEvent(false)
                }

                dir = if (dir == 1) {
                    -1
                } else {
                    1
                }

                true
            }
        }
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {

    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (!disallowIntercept) {
            recyclerView?.parent?.requestDisallowInterceptTouchEvent(true)
        }
    }

    companion object {
        private const val TAG = "ScrollTouchListener"
    }

}

/*
*
val listener = object : OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    val action = e.action
                    if (recyclerview.canScrollHorizontally(RecyclerView.FOCUS_FORWARD)) {
                        when (action) {
                            MotionEvent.ACTION_MOVE -> rv.parent
                                .requestDisallowInterceptTouchEvent(true)
                        }
                        return false
                    }
                    else {
                        when (action) {
                            MotionEvent.ACTION_MOVE -> rv.parent
                                .requestDisallowInterceptTouchEvent(false)
                        }
                        recyclerview.removeOnItemTouchListener(this)
                        return true
                    }
                }

                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            }
* */