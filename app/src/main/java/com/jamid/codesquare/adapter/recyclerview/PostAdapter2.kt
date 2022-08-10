package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.adapter.comparators.ReferenceItemComparator
import com.jamid.codesquare.data.Post2
import com.jamid.codesquare.data.ReferenceItem
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.listeners.MediaClickListener

class PostAdapter2(
    private val lifecycleOwner: LifecycleOwner,
    private val mediaClickListener: MediaClickListener
): PagingDataAdapter<ReferenceItem, PostViewHolder>(ReferenceItemComparator()) {

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {

        getItem(position)?.let {
            FireUtility.getPost(it.id) { post ->
                post?.let {
                    holder.bind(Post2.Collab(post))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder.newInstance(parent, lifecycleOwner, mediaClickListener)
    }

    companion object {
        private const val TAG ="PostAdapter2"
    }

}