package com.jamid.codesquare.ui.zoomableView

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.view.animation.DecelerateInterpolator
import com.facebook.common.internal.Preconditions
import com.facebook.common.logging.FLog


class AnimatedZoomableController @SuppressLint("NewApi") constructor(val transformGestureDetector: TransformGestureDetector?) :
	AbstractAnimatedZoomableController(transformGestureDetector!!) {
	private val mValueAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)

	@SuppressLint("NewApi")
	override fun setTransformAnimated(
		newTransform: Matrix?,
		durationMs: Long,
		onAnimationComplete: Runnable?
	) {
		FLog.v(logTag, "setTransformAnimated: duration %d ms", durationMs)
		stopAnimation()
		Preconditions.checkArgument(durationMs > 0)
		Preconditions.checkState(!isAnimating)
		isAnimating = true
		mValueAnimator.duration = durationMs
		transform.getValues(startValues)
		newTransform?.getValues(stopValues)
		mValueAnimator.addUpdateListener { valueAnimator ->
			calculateInterpolation(workingTransform, valueAnimator.animatedValue as Float)
			super@AnimatedZoomableController.transform = workingTransform
		}
		mValueAnimator.addListener(
			object : AnimatorListenerAdapter() {
				override fun onAnimationCancel(animation: Animator?) {
					FLog.v(logTag, "setTransformAnimated: animation cancelled")
					onAnimationStopped()
				}

				override fun onAnimationEnd(animation: Animator?) {
					FLog.v(logTag, "setTransformAnimated: animation finished")
					onAnimationStopped()
				}

				private fun onAnimationStopped() {
					onAnimationComplete?.run()
					isAnimating = false
					transformGestureDetector?.restartGesture()
				}
			})
		mValueAnimator.start()
	}

	@SuppressLint("NewApi")
	public override fun stopAnimation() {
		if (!isAnimating) {
			return
		}
		FLog.v(logTag, "stopAnimation")
		mValueAnimator.cancel()
		mValueAnimator.removeAllUpdateListeners()
		mValueAnimator.removeAllListeners()
	}

	override val logTag: Class<*>
		get() = AnimatedZoomableController::class.java

	companion object {

		fun newInstance(): AnimatedZoomableController {
			return AnimatedZoomableController(TransformGestureDetector.newInstance())
		}
	}

	init {
		mValueAnimator.interpolator = DecelerateInterpolator()
	}
}