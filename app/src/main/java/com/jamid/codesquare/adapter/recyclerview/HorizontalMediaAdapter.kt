package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.ViewHolderState
import com.jamid.codesquare.adapter.comparators.MediaItemComparator
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.listeners.HorizontalMediaItemClickListener

class HorizontalMediaAdapter(
    private var shouldShowControls: Boolean = false,
    private val horizontalMediaItemClickListener: HorizontalMediaItemClickListener? = null,
    private val isFullscreen: Boolean = false,
    private val parentEventEmitter: LiveData<ViewHolderState>? = null,
    private val lifecycleOwner: LifecycleOwner,
    private val fragmentTag: String,
): ListAdapter<MediaItem, HorizontalMediaViewHolder>(MediaItemComparator()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalMediaViewHolder {
        return if (isFullscreen) {
            HorizontalMediaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fullscreen_media, parent, false), horizontalMediaItemClickListener, shouldShowControls, parentEventEmitter, lifecycleOwner, fragmentTag)
        } else {
            HorizontalMediaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.media_item, parent, false), horizontalMediaItemClickListener, shouldShowControls, parentEventEmitter, lifecycleOwner, fragmentTag)
        }
    }

    override fun onBindViewHolder(holder: HorizontalMediaViewHolder, position: Int) {
        if (!isFullscreen) {
            holder.bind(getItem(position))
        } else {
            holder.bind1(getItem(position))
        }
    }

    override fun onViewAttachedToWindow(holder: HorizontalMediaViewHolder) {
        super.onViewAttachedToWindow(holder)
        Log.d(TAG, "onViewAttachedToWindow: View has been attached to window, going to play video")
        holder.playVideo()
    }

    override fun onViewDetachedFromWindow(holder: HorizontalMediaViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.pauseVideo()
    }

    companion object {
        private const val TAG = "HorizontalMediaAdapter"
    }

}