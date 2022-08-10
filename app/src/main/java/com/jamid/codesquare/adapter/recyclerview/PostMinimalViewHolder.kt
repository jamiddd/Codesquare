package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.data.PostMinimal2
import com.jamid.codesquare.databinding.PostMiniItemBinding
import com.jamid.codesquare.getTextForTime
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.PostClickListener

class PostMinimalViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val postClickListener = view.context as PostClickListener
    private lateinit var binding: PostMiniItemBinding
    init {
        Log.d("Something", "Simple: ")
    }
    fun bind(post: PostMinimal2) {

        binding = PostMiniItemBinding.bind(view)

        FireUtility.checkIfPostAssociatedWithBlockedUser(post) { isBlocked ->
            if (isBlocked != null) {
                if (isBlocked) {
                    binding.root.hide()
                } else {
                    //
                }
            } else {
                binding.root.hide()
            }
        }

        binding.miniProjectThumbnail.setImageURI(post.images.first())
        binding.miniProjectUserImg.setImageURI(post.creator.photo)

        binding.miniProjectContent.text = post.content
        binding.miniProjectName.text = post.name

        val infoText = "${post.creator.name} • ${getTextForTime(post.createdAt)}"
        binding.miniProjectInfo.text = infoText

        FireUtility.getUser(post.creator.userId) { creator ->
            creator?.let {
                binding.miniProjectOption.setOnClickListener {
                    postClickListener.onPostOptionClick(post, creator)
                }
            }
        }

        binding.root.setOnClickListener {
            postClickListener.onPostClick(post)
        }
    }
}