package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.PostComparator
import com.jamid.codesquare.data.Post

class PostAdapter: PagingDataAdapter<Post, SuperPostViewHolder>(PostComparator()) {

    override fun onBindViewHolder(holderSuper: SuperPostViewHolder, position: Int) {
        holderSuper.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuperPostViewHolder {
        return if (viewType == 0) {
            PostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false))
        } else {
            AdViewHolderSuper(LayoutInflater.from(parent.context).inflate(R.layout.custom_post_ad, parent, false))
        }
    }

    override fun onViewAttachedToWindow(holder: SuperPostViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is PostViewHolder && holder.hasAttachedOnce) {
            holder.setLikeBtn2()
            holder.setSaveBtn2()
        }
    }

    override fun onViewDetachedFromWindow(holder: SuperPostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is PostViewHolder) {
            holder.likeListener?.remove()
            holder.saveListener?.remove()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val project = getItem(position)
        return if (project != null) {
            if (!project.isAd) {
                0
            } else {
                1
            }
        } else {
            super.getItemViewType(position)
        }
    }
}