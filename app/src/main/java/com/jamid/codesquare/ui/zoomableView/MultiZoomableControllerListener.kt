package com.jamid.codesquare.ui.zoomableView

import android.graphics.Matrix
import java.util.*


/**
 * An implementation of [ZoomableController.Listener] that allows multiple child listeners to
 * be added and notified about [ZoomableController] events.
 */
class MultiZoomableControllerListener : ZoomableController.Listener {
	private val mListeners: MutableList<ZoomableController.Listener> = ArrayList()

	@Synchronized
	override fun onTransformBegin(transform: Matrix?) {
		for (listener in mListeners) {
			listener.onTransformBegin(transform)
		}
	}

	@Synchronized
	override fun onTransformChanged(transform: Matrix?) {
		for (listener in mListeners) {
			listener.onTransformChanged(transform)
		}
	}

	@Synchronized
	override fun onTransformEnd(transform: Matrix?) {
		for (listener in mListeners) {
			listener.onTransformEnd(transform)
		}
	}

	@Synchronized
	fun addListener(listener: ZoomableController.Listener) {
		mListeners.add(listener)
	}

	@Synchronized
	fun removeListener(listener: ZoomableController.Listener) {
		mListeners.remove(listener)
	}
}