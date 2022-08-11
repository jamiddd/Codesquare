package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.Post2Comparator
import com.jamid.codesquare.data.Post2
import com.jamid.codesquare.listeners.MediaClickListener
import com.jamid.codesquare.listeners.PostClickListener

class PostAdapter3(
    private val lifecycleOwner: LifecycleOwner,
    private val mediaClickListener: MediaClickListener,
    private val listener: PostClickListener? = null,
) : ListAdapter<Post2, SuperPostViewHolder>(Post2Comparator()) {
    init {
        Log.d("Something", "Simple: ")
    }
    var shouldShowJoinButton = true
    var allowContentClick = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuperPostViewHolder {
        return if (viewType == 1) {
            AdViewHolderSuper(LayoutInflater.from(parent.context).inflate(R.layout.custom_post_ad, parent, false), listener)
        } else {
            PostViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false),
                lifecycleOwner,
                mediaClickListener,
                listener
            ).apply {
                shouldShowJoinBtn = shouldShowJoinButton
                shouldAllowContentClick = allowContentClick
            }
        }
    }

    override fun onBindViewHolder(holder: SuperPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /*override fun onViewRecycled(holder: SuperPostViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PostViewHolder) {
            holder.itemView.findViewById<MediaRecyclerView>(R.id.post_media_recycler)?.releasePlayer()
        }
    }*/

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Post2.Advertise -> 1
            is Post2.Collab -> 0
            null -> super.getItemViewType(position)
        }
    }

    /*override fun onViewAttachedToWindow(holder: SuperPostViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is PostViewHolder) {
            Log.d(MediaAdapter.TAG, "onParentViewAttachedToWindow")
            holder.play()
        }
    }

    override fun onViewDetachedFromWindow(holder: SuperPostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.d(MediaAdapter.TAG, "onParentViewDetachedFromWindow")
        if (holder is PostViewHolder) {
            holder.pause()
        }
    }*/

}