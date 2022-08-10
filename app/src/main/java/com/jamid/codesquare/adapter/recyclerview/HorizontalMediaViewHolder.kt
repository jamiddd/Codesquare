package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.jamid.codesquare.ViewHolderState
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.databinding.FullscreenMediaBinding
import com.jamid.codesquare.databinding.MediaItemBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.HorizontalMediaItemClickListener
import com.jamid.codesquare.show
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener
import com.jamid.codesquare.ui.zoomableView.ZoomableDraweeView
import com.jamid.codesquare.video

class HorizontalMediaViewHolder(
    val view: View,
    private val horizontalMediaItemClickListener: HorizontalMediaItemClickListener? = null,
    private val shouldShowControls: Boolean = false,
    private val parentEventEmitter: LiveData<ViewHolderState>? = null,
    private val lifecycleOwner: LifecycleOwner,
    private val fragmentTag: String
): RecyclerView.ViewHolder(view) {
    init {
        Log.d("Something", "Simple: ")
    }
    companion object {
        private const val TAG = "HorizontalMediaViewHolder"
    }

    var mediaItem: MediaItem? = null
    var isViewAttached = false

    var imageView: SimpleDraweeView? = null
    var zoomView: ZoomableDraweeView? = null
    var volumeState: TextView? = null


  /*  private val playbackStateListener: Player.Listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> progressBar?.show()
                ExoPlayer.STATE_IDLE,
                ExoPlayer.STATE_ENDED,
                ExoPlayer.STATE_READY -> progressBar?.hide()
                else -> progressBar?.hide()
            }
        }
    }*/

    private lateinit var binding: MediaItemBinding

    fun bind(media: MediaItem) {

        mediaItem = media
        binding = MediaItemBinding.bind(view)

        imageView = binding.imageView

       /* volumeState?.isSelected = !ExoPlayerProvider.isVolumeMuted*/


        if (media.type == video) {
            setVideo()
        } else {
            setImage()
        }

        volumeState?.setOnClickListener {
           /* ExoPlayerProvider.toggleVolumeState()
            volumeState?.isSelected = volumeState?.isSelected != true*/
        }

        binding.root.setOnClickListener {

            /*if (player?.isPlaying == true) {
                if (media.type == video && horizontalMediaItemClickListener != null) {
                    releasePlayer()
                }
                horizontalMediaItemClickListener?.onMediaItemClick(media, absoluteAdapterPosition)
            } else {
                playVideo()
            }*/
        }

        setParentEventListener()
    }

    @OptIn(ExperimentalPagingApi::class)
    private fun setParentEventListener() {
        parentEventEmitter?.observe(lifecycleOwner) { parentState ->
            val state = parentState ?: return@observe
            when (state) {
                ViewHolderState.STATE_RECYCLED -> {
                    if (mediaItem!!.type == video) {
                        /*ExoPlayerProvider.pause(mediaItem!!.url, fragmentTag)*/
                    }
                }
                ViewHolderState.STATE_ATTACHED -> {
                    if (isViewAttached) {
                        if (mediaItem!!.type == video) {
                            initializePlayer(shouldShowControls)
                            /*ExoPlayerProvider.play(mediaItem!!.url, fragmentTag, playbackStateListener)*/
                        }
                    }
                }
                ViewHolderState.STATE_DETACHED -> {
                    if (mediaItem!!.type == video) {
                        /*ExoPlayerProvider.pause(mediaItem!!.url, fragmentTag)*/
                    }
                }
            }
        }
    }

    private fun setImage() {

        imageView?.show()
        zoomView?.show()

        val media = mediaItem

        if (media != null) {

            imageView?.let {

                val imageRequest = ImageRequest.fromUri(media.url)

                val controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .build()

                it.controller = controller
            }

            zoomView?.let {

                it.setAllowTouchInterceptionWhileZoomed(false)

                val imageRequest = ImageRequest.fromUri(media.url)
                val controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .setCallerContext(this)
                    .build()

                val multiGestureListener = MultiGestureListener()
                multiGestureListener.addListener(TapListener(it))
                multiGestureListener.addListener(DoubleTapGestureListener(it))
                it.setTapListener(multiGestureListener)
                it.controller = controller
            }

        }

        imageView?.setOnClickListener {
            if (!shouldShowControls) {
                horizontalMediaItemClickListener?.onMediaItemClick(mediaItem!!, bindingAdapterPosition)
            }
        }

    }

    private fun setVideo() {
        imageView?.hide()
        zoomView?.hide()
        volumeState?.show()
//        initializePlayer(shouldShowControls)
    }

    private fun initializePlayer(shouldShowControls: Boolean = false) {
        /*val media = mediaItem
        if (media != null && media.type == video) {
            player = ExoPlayer.Builder(view.context)
                .build()
                .also { exoPlayer ->
                    playerView?.player = exoPlayer
                    playerView?.useController = shouldShowControls
                    playerView?.setShowNextButton(false)
                    playerView?.setShowPreviousButton(false)
                    playerView?.setShowFastForwardButton(false)
                    playerView?.setShowRewindButton(false)
                    playerView?.setKeepContentOnPlayerReset(true)

                    val mediaItem = androidx.media3.common.MediaItem.Builder()
                        .setUri(mediaItem!!.url)
                        .setMimeType(MimeTypes.APPLICATION_MP4)
                        .build()

                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.addListener(playbackStateListener)
                    exoPlayer.playWhenReady = !shouldShowControls
                    if (shouldShowControls) {
                        exoPlayer.playWhenReady = false
                    }
                    exoPlayer.seekTo(0, 0)
                    exoPlayer.prepare()
                }

            playerView?.setAspectRatioListener(aspectRatioListener)
        }*/

       /* val exoPlayer = ExoPlayerProvider.provide()
        exoPlayer?.let {
            playerView?.player = it
            ExoPlayerProvider.play(mediaItem!!.url, fragmentTag, playbackStateListener)
            playerView?.useArtwork = true
            playerView?.useController = true
            playerView?.setShowRewindButton(false)
            playerView?.setShowFastForwardButton(false)
            playerView?.setShowNextButton(false)
            playerView?.setShowPreviousButton(false)
            playerView?.setKeepContentOnPlayerReset(true)
        }

        val controls = playerView?.findViewById<View>(R.id.video_controls_root)
        controls?.setOnClickListener {
            if (exoPlayer?.isPlaying != true) {
                exoPlayer?.play()
            }
        }*/

    }

    fun playVideo() {
        if (mediaItem != null) {
            if (mediaItem!!.type == video) {
                /*ExoPlayerProvider.play(mediaItem!!.url, fragmentTag)*/
            }
        } else {
            Log.d(TAG, "playVideo: MediaItem is null")
        }
    }

    fun pauseVideo() {
        if (mediaItem != null) {
            if (mediaItem!!.type == video) {
                /*ExoPlayerProvider.pause(mediaItem!!.url, fragmentTag)*/
            }
        } else {
            Log.d(TAG, "pauseVideo: Media item is null")
        }
    }

  /*  fun playVideo() {
        val media = mediaItem
        if (media != null && media.type == video) {
            if (player == null) {
                initializePlayer(shouldShowControls)
            } else {
                player?.play()
            }
        }
    }*/

  /*  fun pauseVideo() {
        val media = mediaItem
        if (media != null && media.type == video) {
            if (player == null) {
                initializePlayer(shouldShowControls)
            } else {
                player?.pause()
            }
        }
    }*/

    /*private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
        player = null
    } */

    /* For fullscreen media */
    private lateinit var binding1: FullscreenMediaBinding

    fun bind1(media: MediaItem) {

        binding1 = FullscreenMediaBinding.bind(view)
        mediaItem = media

        zoomView = binding1.fullscreenImage

        if (media.type == video) {
            setVideo()
        } else {
            setImage()
        }

        setParentEventListener()
    }

}