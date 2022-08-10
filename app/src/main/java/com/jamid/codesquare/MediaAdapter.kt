package com.jamid.codesquare

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.jamid.codesquare.adapter.comparators.MediaItemComparator
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.databinding.MediaItem2Binding
import com.jamid.codesquare.listeners.MediaClickListener
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener

class MediaAdapter(
    val postId: String,
    private val isZoomEnabled: Boolean = false,
    private val mediaClickListener: MediaClickListener
) : ListAdapter<MediaItem, MediaViewHolder>(MediaItemComparator()) {

    companion object {
        const val TAG = "MediaAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder.newInstance(R.layout.media_item_2, parent, postId, isZoomEnabled, mediaClickListener)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item == null) {
            super.getItemViewType(position)
        } else {
            if (item.type == image) {
                if (isZoomEnabled) {
                    2
                } else {
                    0
                }
            } else {
                1
            }
        }
    }

}

class MediaViewHolder(
    val view: View,
    val postId: String,
    private val isZoomEnabled: Boolean,
    private val mediaClickListener: MediaClickListener
) : RecyclerView.ViewHolder(view) {

    var thumbnail: ImageView? = null
    var progressBar: CircularProgressIndicator? = null
    var volumeControl: ImageView? = null
    var parent = itemView
    var url: String? = null
    var type: String = video

    fun bind(item: MediaItem) {
        url = item.url
        val binding = MediaItem2Binding.bind(view)
        type = item.type

        parent.tag = this
        progressBar = binding.progressBar

        if (item.type == image) {
            if (isZoomEnabled) {

                binding.thumbnail.hide()
                binding.thumbnailZoom.show()

                val controllerListener = FrescoImageControllerListener { _, _ ->
                    progressBar?.hide()
                    Log.d(TAG, "bind: Hiding progressbar")
                }

                val imageRequest = ImageRequest.fromUri(item.url)
                val controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .setControllerListener(controllerListener)
                    .build()

                binding.thumbnailZoom.controller = controller

                val multiGestureListener = MultiGestureListener()
//                multiGestureListener.addListener(TapListener(binding.thumbnailZoom))
                multiGestureListener.addListener(DoubleTapGestureListener(binding.thumbnailZoom))

                binding.thumbnailZoom.setAllowTouchInterceptionWhileZoomed(false)

                binding.thumbnailZoom.setTapListener(object: TapListener(binding.thumbnailZoom) {
                    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                        mediaClickListener.onMediaClick(MediaItemWrapper(item, false, -1), bindingAdapterPosition)
                        return super.onSingleTapConfirmed(e)
                    }
                })


            } else {
                if (view.context.isNightMode()) {
                    binding.mediaContainer.setBackgroundColor(Color.BLACK)
                } else {
                    binding.mediaContainer.setBackgroundColor(Color.WHITE)
                }

                binding.thumbnail.show()
                binding.thumbnailZoom.hide()
                progressBar?.show()
                val controllerListener = FrescoImageControllerListener { _, _ ->
                    progressBar?.hide()
                    Log.d(TAG, "bind: Hiding progressbar")
                }

                val request = ImageRequest.fromUri(item.url)
                val controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(request)
                    .setControllerListener(controllerListener)
                    .build()

                binding.thumbnail.controller = controller

                binding.thumbnail.setOnClickListener {
                    mediaClickListener.onMediaClick(MediaItemWrapper(item, false, 0), bindingAdapterPosition)
                }

            }
        } else {
            binding.thumbnail.hide()
            binding.thumbnailZoom.hide()
        }

        /*this.requestManager
            .load(mediaObject.getThumbnail())
            .into(thumbnail);*/
    }

    companion object {
        fun newInstance(
            @LayoutRes layout: Int,
            parent: ViewGroup,
            postId: String,
            isZoomEnabled: Boolean,
            mediaClickListener: MediaClickListener
        ) =
            MediaViewHolder(
                LayoutInflater.from(parent.context).inflate(layout, parent, false),
                postId,
                isZoomEnabled,
                mediaClickListener
            )
    }

}