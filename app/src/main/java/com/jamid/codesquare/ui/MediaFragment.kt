package com.jamid.codesquare.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FullscreenMediaBinding
import com.jamid.codesquare.getTextForSizeInBytes
import com.jamid.codesquare.hide
import com.jamid.codesquare.show
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener
import java.text.SimpleDateFormat
import java.util.*

class MediaFragment: Fragment() {

    private lateinit var binding: FullscreenMediaBinding
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

//    private var player: ExoPlayer? = null
    private var videoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FullscreenMediaBinding.inflate(inflater)
        return binding.root
    }

   /* private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> {
                    binding.fullscreenMediaProgress.hide()
                    "ExoPlayer.STATE_READY     -"
                }
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> {
                    binding.fullscreenMediaProgress.hide()
                    "UNKNOWN_STATE             -"
                }
            }
            Log.d(TAG, "changed state to $stateString")
        }
    }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mediaItem = arguments?.getParcelable<MediaItem>("mediaItem") ?: return
        val message = arguments?.getParcelable<Message>("message")

        /*val screenWidth = getWindowWidth()
        val heightInPx = ((screenWidth * mediaItem.height) / mediaItem.width).toInt()*/

        if (mediaItem.type == "video") {

            binding.fullscreenMediaProgress.show()

            hideImageRelatedStuff()
            videoUri = mediaItem.url.toUri()

//            initializePlayer()

        }

        if (mediaItem.type == "image") {
            hideVideoRelatedStuff()

            /*binding.fullscreenImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startToStart = binding.fullscreenMediaContainer.id
                endToEnd = binding.fullscreenMediaContainer.id
                topToTop = binding.fullscreenMediaContainer.id
                bottomToBottom = binding.fullscreenMediaContainer.id

                height = heightInPx.toInt()
                width = ConstraintLayout.LayoutParams.MATCH_PARENT
            }*/

            binding.fullscreenImage.apply {
                val imageRequest = ImageRequest.fromUri(mediaItem.url)
                val controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .setCallerContext(this)
                    .build()

                val multiGestureListener = MultiGestureListener()
                multiGestureListener.addListener(TapListener(this))
                multiGestureListener.addListener(DoubleTapGestureListener(this))
                setTapListener(multiGestureListener)

                setController(controller)
            }

            /*viewLifecycleOwner.lifecycleScope.launch {
                delay(500)
                requireActivity().runOnUiThread {
                    binding.fullscreenImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        width = ConstraintLayout.LayoutParams.MATCH_PARENT
                        height = ConstraintLayout.LayoutParams.MATCH_PARENT
                    }
                }
            }*/

        }

       /* binding.fullscreenImage.setOnClickListener {
            onImageViewerClick(binding.fullscreenImage, binding.bottomInfoView)
        }

        binding.fullscreenMediaContainer.setOnClickListener {
            onImageViewerClick(binding.fullscreenImage, binding.bottomInfoView)
        }*/

        if (message != null) {
            binding.bottomInfoView.show()

            val imageViewInfo = "Sent by ${message.sender.name} • " + SimpleDateFormat(
                "hh:mm a, dd/MM/yyyy",
                Locale.UK
            ).format(message.createdAt)

            binding.userTimeInfo.text = imageViewInfo
            binding.imageSize.text = getTextForSizeInBytes(message.metadata!!.size)
        } else {
            binding.bottomInfoView.hide()
        }

      /*  binding.mediaViewToolbar.setNavigationOnClickListener {

            binding.fullscreenImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startToStart = binding.fullscreenMediaContainer.id
                endToEnd = binding.fullscreenMediaContainer.id
                topToTop = binding.fullscreenMediaContainer.id
                bottomToBottom = binding.fullscreenMediaContainer.id
                height = heightInPx.toInt()
                width = ConstraintLayout.LayoutParams.MATCH_PARENT
            }

            lifecycleScope.launch {
                delay(200)
                removeImageViewFragment()
            }
        }*/


    }

   /* @OptIn(ExperimentalPagingApi::class)
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun initializePlayer() {
        if (videoUri != null) {

            val trackSelector = DefaultTrackSelector(activity as MainActivity).apply {
                setParameters(buildUponParameters().setMaxVideoSizeSd())
            }

            player = ExoPlayer.Builder(activity as MainActivity)
                .setTrackSelector(trackSelector)
                .build()
                .also { exoPlayer ->
//                    binding.fullscreenVideo.player = exoPlayer

                    val mediaItem = androidx.media3.common.MediaItem.fromUri(videoUri!!)
                    exoPlayer.setMediaItem(mediaItem)

                    exoPlayer.playWhenReady = playWhenReady
                    exoPlayer.seekTo(currentItem, playbackPosition)

                    exoPlayer.addListener(playbackStateListener())

                    exoPlayer.prepare()
                }
        }
    }*/

   /* override fun onResume() {
        super.onResume()
        initializePlayer()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }*/

   /* private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }*/

    private fun hideVideoRelatedStuff() {
//        binding.fullscreenVideo.hide()
    }

    private fun hideImageRelatedStuff() {
        binding.fullscreenImage.hide()
    }

    companion object {
        private const val TAG = "MediaFragment"

        fun newInstance(mediaItem: MediaItem, message: Message? = null) =
            MediaFragment().apply {
                arguments = bundleOf("mediaItem" to mediaItem, "message" to message)
            }

    }

}