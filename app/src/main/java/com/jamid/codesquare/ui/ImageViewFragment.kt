package com.jamid.codesquare.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.Fragment
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentImageViewBinding
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener
import java.text.SimpleDateFormat
import java.util.*

class ImageViewFragment: Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentImageViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 250
            scrimColor = Color.TRANSPARENT
        }
        sharedElementReturnTransition = MaterialContainerTransform().apply {
            duration = 250
            scrimColor = Color.TRANSPARENT
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageViewBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val image = arguments?.getString("fullscreenImage") ?: return
        val transitionName = arguments?.getString("transitionName") ?: return
        val ext = arguments?.getString("ext") ?: return

        val message = arguments?.getParcelable<Message>("message")

        ViewCompat.setTransitionName(binding.fullscreenImage, transitionName)

        binding.fullscreenImage.apply {

            val controller = if (ext == ".webp") {
                Fresco.newDraweeControllerBuilder()
                    .setUri(image)
                    .setAutoPlayAnimations(true)
                    .build()
            } else {
                val imageRequest = ImageRequest.fromUri(image)
                Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .setCallerContext(this)
                    .build()
            }

            startPostponedEnterTransition()

            val multiGestureListener = MultiGestureListener()
//            multiGestureListener.addListener(FlingListener(this@ImageViewFragment))
            multiGestureListener.addListener(TapListener(this))
            multiGestureListener.addListener(DoubleTapGestureListener(this))
            setTapListener(multiGestureListener)

            setController(controller)

            setOnClickListener(this@ImageViewFragment)
        }

        if (message?.metadata != null) {
            binding.bottomInfoView.show()
            val sentByText = "Sent by ${message.sender.name} • " + SimpleDateFormat("hh:mm a, dd/MM/yyyy", Locale.UK).format(message.createdAt)
            binding.userTimeInfo.text = sentByText
            binding.imageSize.text = getTextForSizeInBytes(message.metadata!!.size)
        } else {
            binding.bottomInfoView.hide()
        }

    }

    override fun onClick(p0: View?) {
        val appbar = activity?.findViewById<AppBarLayout>(R.id.main_appbar)!!

        if (appbar.translationY == 0f) {
            appbar.slideUp(convertDpToPx(100).toFloat())
            binding.fullscreenImage.setBackgroundColor(Color.BLACK)

            binding.bottomInfoView.slideDown(convertDpToPx(150).toFloat())

            hideSystemUI()

        } else {
            appbar.slideReset()
            if (!isNightMode()) {
                binding.fullscreenImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lightest_grey))
            }

            binding.bottomInfoView.slideReset()

            showSystemUI()
        }
    }

    private fun hideSystemUI() {
        val activity = requireActivity()
        val view = activity.findViewById<View>(R.id.main_container_root)
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, view).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        val activity = requireActivity()
        val view = activity.findViewById<View>(R.id.main_container_root)
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        WindowInsetsControllerCompat(activity.window, view).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        super.onDestroy()
        val appbar = activity?.findViewById<AppBarLayout>(R.id.main_appbar)!!
        showSystemUI()
        appbar.slideReset()
        if (!isNightMode()) {
            binding.fullscreenImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lightest_grey))
        }

        binding.bottomInfoView.slideReset()
    }

    companion object {
        fun newInstance(title: String, transitionName: String, image: String, ext: String) =
            ImageViewFragment().apply {
                arguments = bundleOf("title" to title, "transitionName" to transitionName, "fullscreenImage" to image, "ext" to ext)
            }
    }


}