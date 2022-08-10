package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.Post2Comparator
import com.jamid.codesquare.data.Post2
import com.jamid.codesquare.listeners.MediaClickListener

class PostAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val mediaClickListener: MediaClickListener
): PagingDataAdapter<Post2, SuperPostViewHolder>(Post2Comparator()) {

    var shouldShowJoinButton = true
    init {
        Log.d("Something", "Simple: ")
    }
    override fun onBindViewHolder(holderSuper: SuperPostViewHolder, position: Int) {
        holderSuper.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuperPostViewHolder {
        return when (viewType) {
            1 -> {
                AdViewHolderSuper(LayoutInflater.from(parent.context).inflate(R.layout.custom_post_ad, parent, false))
            }
            else -> {
                PostViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false),
                    lifecycleOwner,
                    mediaClickListener
                ).apply {
                    shouldShowJoinBtn = shouldShowJoinButton
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Post2.Advertise -> 1
            is Post2.Collab -> 0
            null -> super.getItemViewType(position)
        }
    }

    companion object {
         private const val TAG = "PostAdapter"
     }

}
