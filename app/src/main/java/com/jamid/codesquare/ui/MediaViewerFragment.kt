package com.jamid.codesquare.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.addCallback
import androidx.core.animation.doOnEnd
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.transition.Fade
import androidx.transition.Fade.IN
import androidx.transition.Fade.OUT
import com.jamid.codesquare.*
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentMediaViewerBinding
import com.jamid.codesquare.listeners.MediaClickListener

class MediaViewerFragment: BaseFragment<FragmentMediaViewerBinding>(), MediaClickListener {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentMediaViewerBinding {
        return FragmentMediaViewerBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade(IN)
        exitTransition = Fade(OUT)
        reenterTransition = null
        returnTransition = null
    }

    fun onBackPressed() {
        val orientation = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // code for portrait mode
            findNavController().navigateUp()
        } else {
            // code for landscape mode
            (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }


    private val controllerVisibilityListener = PlayerView.ControllerVisibilityListener {
        if (it == View.VISIBLE) {
            binding.mediaViewerToolbar.slideReset().doOnEnd {
                onVisibilityChange()
            }
        } else {
            val dy = resources.getDimension(R.dimen.appbar_slide_translation)
            binding.mediaViewerToolbar.slideUp(dy).doOnEnd {
                onVisibilityChange()
            }
        }
    }

    private fun onVisibilityChange() {
        if (binding.mediaViewerToolbar.translationY == 0f) {
            binding.mediaViewerToolbar.slideReset()
            activity.showSystemUI()
        } else {
            val dy = resources.getDimension(R.dimen.appbar_slide_translation)
            binding.mediaViewerToolbar.slideUp(dy)
            activity.hideSystemUI()
        }
    }


    @OptIn(ExperimentalPagingApi::class)
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = arguments?.getParcelableArrayList<MediaItem>("list") ?: return
        val currentPosition = arguments?.getInt("current_position") ?: 0

        activity.removeLightStatusBar()
        activity.setNavigationBarColor(Color.BLACK)

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onBackPressed()
        }

        val mediaAdapter = MediaAdapter("", true, this)

        val manager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        manager.recycleChildrenOnDetach = true

        val helper = LinearSnapHelper()

        binding.navigateUpBtn.setOnClickListener {
            onBackPressed()
        }

        binding.fullscreenMediaRecycler.apply {
            layoutManager = manager
            setZoomEnabled(true)
            setVideoControllerVisibilityListener(controllerVisibilityListener)

            mLifecycleOwner = viewLifecycleOwner
            this.setPlayerResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)

            setMediaObjects(list.map {
                it.url
            })

            adapter = mediaAdapter

            if (onFlingListener == null) {
                helper.attachToRecyclerView(this)
            }
        }

        mediaAdapter.submitList(list)

        binding.fullscreenMediaRecycler.post {
            binding.fullscreenMediaRecycler.scrollToPosition(currentPosition)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.showSystemUI()
        activity.setLightStatusBar()
        activity.setNavigationBarColor(null)
    }

    companion object {
        private const val TAG = "MediaViewer"
        const val FRAGMENT_END = 6
    }

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {

    }

    override fun onMediaMessageItemClick(message: Message) {

    }

    override fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {
        if (binding.mediaViewerToolbar.translationY == 0f) {
            val dy = resources.getDimension(R.dimen.appbar_slide_translation)
            binding.mediaViewerToolbar.slideUp(dy)
            activity.hideSystemUI()
        } else {
            binding.mediaViewerToolbar.slideReset()
            activity.showSystemUI()
        }
    }

}