package com.jamid.codesquare.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.Display
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
// something simple

class MediaRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    private enum class VolumeState {
        ON, OFF
    }

    // ui
    private var thumbnail: ImageView? = null
    private var volumeControl: ImageView? = null
    private var progressBar: CircularProgressIndicator? = null
    private var viewHolderParent: View? = null
    private var frameLayout: FrameLayout? = null
    private var playerView: PlayerView? = null
    private var exoPlayer: ExoPlayer? = null

    // vars
    private var mediaObjects: List<String> = emptyList()
    private var videoSurfaceDefaultHeight = 0
    private var screenDefaultHeight = 0
    private var mContext: Context? = null
    private var playPosition = -1
    private var isVideoViewAdded = false

    private var isZoomEnabled = false

    private var mediaCounterText: TextView? = null

    fun setZoomEnabled(state: Boolean) {
        isZoomEnabled = state
    }

    fun setMediaCounterText(textView: TextView?) {
        mediaCounterText = textView
    }

    // controlling playback state
    private var volumeState: VolumeState? = null
    var mLifecycleOwner: LifecycleOwner? = null

    private val onScrollListener = object : OnScrollListener() {

        var totalScroll = 0

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            totalScroll += abs(dx)

            if (totalScroll > recyclerView.measuredWidth/2) {

                val pos = if (sign(dx.toFloat()) == 1f) {
                    (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                } else {
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                }

                if (pos != -1) {
                    val cText = "${pos + 1}/${adapter?.itemCount ?: 0}"
                    mediaCounterText?.text = cText
                }
            } else {
                Log.d(PostViewHolder.TAG, "onScrolled: $totalScroll - ${recyclerView.measuredWidth/5}")
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            when (newState) {
                SCROLL_STATE_DRAGGING -> {
                    mediaCounterText?.fadeIn()?.doOnEnd {
                        fadeOutCounterText()
                    }
                }
                SCROLL_STATE_IDLE -> {
                    totalScroll = 0

                    val pos = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    if (pos != -1) {
                        val cText = "${pos + 1}/${adapter?.itemCount ?: 0}"
                        mediaCounterText?.text = cText
                    }

                    thumbnail?.visibility = VISIBLE
                    playVideo()
                }
            }
        }
    }

    private var counterHideJob: Job? = null

    private fun fadeOutCounterText() {
        counterHideJob?.cancel()
        counterHideJob = mLifecycleOwner?.lifecycleScope?.launch {
            delay(5000)

            // after 5 seconds fadeout the text
            mediaCounterText?.fadeOut()
        }
    }


    private val onChildAttachStateChangeListener =
        object : OnChildAttachStateChangeListener {
            override fun onChildViewDetachedFromWindow(view: View) {
                if (viewHolderParent != null && viewHolderParent == view) {
                    resetVideoView()
                } else {
                    Log.d(TAG, "onChildViewDetachedFromWindow: Either viewholderparent is null or viewholderparent is not the view to be detached")
                }
            }

            override fun onChildViewAttachedToWindow(view: View) {
                mLifecycleOwner?.lifecycleScope?.launch {
                    delay(500)
                    (context as? Activity)?.runOnUiThread {
                        onScrollListener.onScrollStateChanged(this@MediaRecyclerView, SCROLL_STATE_IDLE)
                    }
                }
            }
        }

    init {
        init(context)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun init(context: Context) {
        this.mContext = context.applicationContext
        val display: Display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()

        display.getSize(point)
        videoSurfaceDefaultHeight = point.x
        screenDefaultHeight = point.y
        playerView = PlayerView(this.context).apply {
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShowRewindButton(false)
            setShowFastForwardButton(false)
            setShowNextButton(false)
            setShowPreviousButton(false)
        }

        val trackSelector = DefaultTrackSelector(context)

        exoPlayer = ExoPlayer.Builder(context).setTrackSelector(trackSelector).build()
        exoPlayer?.let { exoPlayer ->
            // TODO("Make controller dynamic")
            playerView?.useController = true
            playerView?.player = exoPlayer
        }

        setVolumeControl(VolumeState.ON)
        addOnScrollListener(onScrollListener)
        addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        progressBar?.visibility = VISIBLE
                    }
                    Player.STATE_ENDED -> {
                        exoPlayer?.seekTo(0)
                    }
                    Player.STATE_IDLE -> {}
                    Player.STATE_READY -> {
                        progressBar?.visibility = GONE
                        addVideoView()
                    }
                }
            }
        })
    }

    private var controllerVisibilityListener: PlayerView.ControllerVisibilityListener? = null

    fun setVideoControllerVisibilityListener(listener: PlayerView.ControllerVisibilityListener?) {
        controllerVisibilityListener = listener
    }


    private val fullscreenListener = PlayerView.FullscreenButtonClickListener {
        if (it) {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (context as? MainActivity)?.showSystemUI()
        } else {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            (context as? MainActivity)?.hideSystemUI()
        }
    }


    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playVideo() {
        if (playerView == null) {
            return
        }

        val manager = layoutManager as LinearLayoutManager
        val currentPos = manager.findFirstVisibleItemPosition()

        // remove any old surface views from previously playing videos
        playerView?.visibility = INVISIBLE
        removeVideoView(playerView)

        val child = getChildAt(0) ?: return

        val holder = child.tag as? MediaViewHolder ?: return

        if (holder.type == image) {
            return
        }

        thumbnail = holder.thumbnail
        progressBar = holder.progressBar
        volumeControl = holder.volumeControl
        viewHolderParent = holder.itemView
        frameLayout = holder.itemView.findViewById(R.id.media_container)
        playerView?.player = exoPlayer
        viewHolderParent?.setOnClickListener(videoViewClickListener)

        if (context.isNightMode()) {
            playerView?.findViewById<View>(R.id.exo_bottom_bar)?.setBackgroundColor(Color.TRANSPARENT)
            playerView?.findViewById<View>(R.id.exo_overlay)?.setBackgroundColor(Color.TRANSPARENT)
        } else {
            playerView?.findViewById<View>(R.id.exo_bottom_bar)?.setBackgroundColor(Color.TRANSPARENT)
            playerView?.findViewById<View>(R.id.exo_overlay)?.setBackgroundColor(Color.TRANSPARENT)
        }

        if (isZoomEnabled) {
            playerView?.setFullscreenButtonClickListener(fullscreenListener)

            playerView?.setControllerVisibilityListener(controllerVisibilityListener)

        }

        val mediaUrl = holder.url
        mediaUrl?.let {
            exoPlayer?.let { exoplayer ->
                exoplayer.setMediaItem(MediaItem.fromUri(mediaUrl))
                exoplayer.prepare()
                exoplayer.playWhenReady = true
            }
        }


    }

    private val videoViewClickListener = OnClickListener { toggleVolume() }

    /**
     * Returns the visible region of the video surface on the screen.
     * if some is cut off, it will return less than the @videoSurfaceDefaultHeight
     * @param playPosition
     * @return
     */
    private fun getVisibleVideoSurfaceHeight(playPosition: Int): Int {
        val manager = layoutManager as? LinearLayoutManager
        return if (manager != null) {
            val at = playPosition - manager.findFirstVisibleItemPosition()
            val child = getChildAt(at) ?: return 0
            val location = IntArray(2)
            child.getLocationInWindow(location)
            if (location[1] < 0) {
                location[1] + videoSurfaceDefaultHeight
            } else {
                screenDefaultHeight - location[1]
            }
        } else {
            0
        }
    }

    // Remove the old player
    private fun  removeVideoView(playerView: PlayerView?) {
        if (playerView != null) {
            val parent = playerView.parent as? ViewGroup ?: return
            val index = parent.indexOfChild(playerView)
            if (index >= 0) {
                parent.removeViewAt(index)
                isVideoViewAdded = false
                viewHolderParent?.setOnClickListener(null)
            }
        }
    }

    private fun addVideoView() {
        if (frameLayout!!.indexOfChild(playerView) != -1)
            return

        frameLayout?.addView(playerView)
        isVideoViewAdded = true

        playerView?.apply {
            requestFocus()
            visibility = VISIBLE
            alpha = 1f
        }

        thumbnail?.visibility = GONE
    }

    private fun resetVideoView() {
        exoPlayer?.stop()
        removeVideoView(playerView)
        playPosition = -1
        playerView?.visibility = INVISIBLE
        thumbnail?.visibility = VISIBLE
    }

    fun releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer?.release()
            exoPlayer = null
        }
        viewHolderParent = null
    }

    private fun toggleVolume() {
        if (exoPlayer != null) {
            if (volumeState == VolumeState.OFF) {
                setVolumeControl(VolumeState.ON)
            } else if (volumeState == VolumeState.ON) {
                setVolumeControl(VolumeState.OFF)
            }
        }
    }

    private fun setVolumeControl(state: VolumeState) {
        volumeState = state
        if (state == VolumeState.OFF) {
            exoPlayer!!.volume = 0f
            animateVolumeControl()
        } else if (state == VolumeState.ON) {
            exoPlayer!!.volume = 1f
            animateVolumeControl()
        }
    }

    private fun animateVolumeControl() {
        if (volumeControl != null) {
            volumeControl?.bringToFront()

            when (volumeState) {
                VolumeState.ON -> {

                }
                VolumeState.OFF -> {

                }
                null -> Log.d(TAG, "animateVolumeControl: NULL")
            }

            volumeControl?.let {
                it.animate()?.cancel()
                it.alpha = 1f
                it.animate()
                    .alpha(0f)
                    .setDuration(600).setStartDelay(1000)
            }
        }
    }

    fun setMediaObjects(mediaObjects: List<String>) {
        this.mediaObjects = mediaObjects
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun setPlayerResizeMode(mode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT) {
        playerView?.resizeMode = mode
    }

    companion object {
        private const val TAG = "VideoPlayerRecyclerView"
    }
}

