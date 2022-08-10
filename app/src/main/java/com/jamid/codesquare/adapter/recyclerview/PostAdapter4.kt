
package com.jamid.codesquare.adapter.recyclerview
/*
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.Post2

class PostAdapter4(val lifecycleOwner: LifecycleOwner, options: FirestorePagingOptions<Post>): FirestorePagingAdapter<Post, SuperPostViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuperPostViewHolder {
        return if (viewType == 2) {
            AdViewHolderSuper(LayoutInflater.from(parent.context).inflate(R.layout.custom_post_ad, parent, false))
        } else {
            PostViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false),
                lifecycleOwner
            ).apply {
                shouldShowJoinBtn = true
            }
        }
    }

    override fun onBindViewHolder(holder: SuperPostViewHolder, position: Int, model: Post) {
        val post = getItem(position)?.toObject(Post::class.java)
        if (post != null) {
            holder.bind(Post2.Collab(post))
        }
    }

    override fun getItemViewType(position: Int): Int {
        val doc = getItem(position)
        return if (doc != null) {
            val post = doc.toObject(Post::class.java)
            if (post != null) {
                if (post.isAd) {
                    2
                } else {
                    1
                }
            } else {
                super.getItemViewType(position)
            }
        } else {
            super.getItemViewType(position)
        }
    }

}*/
