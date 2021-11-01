package com.jamid.codesquare.ui.zoomableView

import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.hypot


class TransformGestureDetector(multiPointerGestureDetector: MultiPointerGestureDetector) :
	MultiPointerGestureDetector.Listener {
	/** The listener for receiving notifications when gestures occur.  */
	interface Listener {
		/** A callback called right before the gesture is about to start.  */
		fun onGestureBegin(detector: TransformGestureDetector?)

		/** A callback called each time the gesture gets updated.  */
		fun onGestureUpdate(detector: TransformGestureDetector?)

		/** A callback called right after the gesture has finished.  */
		fun onGestureEnd(detector: TransformGestureDetector?)
	}

	private val mDetector: MultiPointerGestureDetector

	private var mListener: Listener? = null

	/**
	 * Sets the listener.
	 *
	 * @param listener listener to set
	 */
	fun setListener(listener: Listener?) {
		mListener = listener
	}

	/** Resets the component to the initial state.  */
	fun reset() {
		mDetector.reset()
	}

	/**
	 * Handles the given motion event.
	 *
	 * @param event event to handle
	 * @return whether or not the event was handled
	 */
	fun onTouchEvent(event: MotionEvent?): Boolean? {
		return event?.let { mDetector.onTouchEvent(it) }
	}

	override fun onGestureBegin(detector: MultiPointerGestureDetector?) {
		if (mListener != null) {
			mListener!!.onGestureBegin(this)
		}
	}

	override fun onGestureUpdate(detector: MultiPointerGestureDetector?) {
		if (mListener != null) {
			mListener!!.onGestureUpdate(this)
		}
	}

	override fun onGestureEnd(detector: MultiPointerGestureDetector?) {
		if (mListener != null) {
			mListener!!.onGestureEnd(this)
		}
	}

	private fun calcAverage(arr: FloatArray, len: Int): Float {
		var sum: Float = 0f
		for (i in 0 until len) {
			sum += arr.get(i)
		}
		return if ((len > 0)) sum / len else 0f
	}

	/** Restarts the current gesture (if any).  */
	fun restartGesture() {
		mDetector.restartGesture()
	}

	/** Gets whether there is a gesture in progress  */
	val isGestureInProgress: Boolean
		get() = mDetector.isGestureInProgress

	/** Gets the number of pointers after the current gesture  */
	val newPointerCount: Int
		get() = mDetector.newPointerCount

	/** Gets the number of pointers in the current gesture  */
	val pointerCount: Int
		get() = mDetector.pointerCount

	/** Gets the X coordinate of the pivot point  */
	val pivotX: Float
		get() = calcAverage(mDetector.startX, mDetector.pointerCount)

	/** Gets the Y coordinate of the pivot point  */
	val pivotY: Float
		get() = calcAverage(mDetector.startY, mDetector.pointerCount)

	/** Gets the X component of the translation  */
	val translationX: Float
		get() = (calcAverage(mDetector.currentX, mDetector.pointerCount)
				- calcAverage(mDetector.startX, mDetector.pointerCount))

	/** Gets the Y component of the translation  */
	val translationY: Float
		get() {
			return (calcAverage(mDetector.currentY, mDetector.pointerCount)
					- calcAverage(mDetector.startY, mDetector.pointerCount))
		}

	/** Gets the scale  */
	val scale: Float
		get() {
			if (mDetector.pointerCount < 2) {
				return 1f
			} else {
				val startDeltaX: Float = mDetector.startX[1] - mDetector.startX[0]
				val startDeltaY: Float = mDetector.startY[1] - mDetector.startY[0]
				val currentDeltaX: Float =
					mDetector.currentX[1] - mDetector.currentX[0]
				val currentDeltaY: Float =
					mDetector.currentY[1] - mDetector.currentY[0]
				val startDist: Float =
					hypot(startDeltaX.toDouble(), startDeltaY.toDouble()).toFloat()
				val currentDist: Float =
					hypot(currentDeltaX.toDouble(), currentDeltaY.toDouble()).toFloat()
				return currentDist / startDist
			}
		}

	/** Gets the rotation in radians  */
	val rotation: Float
		get() {
			if (mDetector.pointerCount < 2) {
				return 0f
			} else {
				val startDeltaX: Float = mDetector.startX[1] - mDetector.startX[0]
				val startDeltaY: Float = mDetector.startY[1] - mDetector.startY[0]
				val currentDeltaX: Float =
					mDetector.currentX[1] - mDetector.currentX[0]
				val currentDeltaY: Float =
					mDetector.currentY[1] - mDetector.currentY[0]
				val startAngle: Float =
					atan2(startDeltaY.toDouble(), startDeltaX.toDouble()).toFloat()
				val currentAngle: Float =
					atan2(currentDeltaY.toDouble(), currentDeltaX.toDouble()).toFloat()
				return currentAngle - startAngle
			}
		}

	companion object {
		/** Factory method that creates a new instance of TransformGestureDetector  */
		fun newInstance(): TransformGestureDetector {
			return TransformGestureDetector(MultiPointerGestureDetector.newInstance())
		}
	}

	init {
		mDetector = multiPointerGestureDetector
		mDetector.setListener(this)
	}
}