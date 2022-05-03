package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.data.PostMinimal2
import com.jamid.codesquare.databinding.PostMiniItemBinding
import com.jamid.codesquare.getTextForTime
import com.jamid.codesquare.listeners.PostClickListener

class PostMinimalViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val projectClickListener = view.context as PostClickListener
    private lateinit var binding: PostMiniItemBinding

    fun bind(post: PostMinimal2) {

        binding = PostMiniItemBinding.bind(view)

        binding.miniProjectThumbnail.setImageURI(post.images.first())
        binding.miniProjectUserImg.setImageURI(post.creator.photo)

        binding.miniProjectContent.text = post.content
        binding.miniProjectName.text = post.name

        val infoText = "${post.creator.name} • ${getTextForTime(post.createdAt)}"
        binding.miniProjectInfo.text = infoText

        binding.miniProjectOption.setOnClickListener {
            projectClickListener.onPostOptionClick(post)
        }

        binding.root.setOnClickListener {
            projectClickListener.onPostClick(post)
        }
    }
}