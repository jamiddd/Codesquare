package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.comparators.PostInviteComparator
import com.jamid.codesquare.adapter.recyclerview.PostInviteAdapter.PostInviteViewHolder
import com.jamid.codesquare.data.PostInvite
import com.jamid.codesquare.databinding.RequestItemBinding
import com.jamid.codesquare.listeners.PostInviteListener

class PostInviteAdapter : PagingDataAdapter<PostInvite, PostInviteViewHolder>(PostInviteComparator()) {

    inner class PostInviteViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private lateinit var binding: RequestItemBinding
        private val postInviteListener = view.context as PostInviteListener

        fun bind(postInvite: PostInvite?) {

            if (postInvite == null)
                return

            binding = RequestItemBinding.bind(view)

            binding.requestProgress.show()
            binding.requestPrimaryAction.disappear()
            binding.requestSecondaryAction.disappear()

            binding.requestTime.text = getTextForTime(postInvite.createdAt)

            binding.requestProjectName.text = postInvite.post.name
            binding.requestImg.setImageURI(postInvite.sender.photo)

            val contentText = "${postInvite.sender.name} has invited you to join their project."
            binding.requestContent.text = contentText

            binding.requestProgress.hide()
            binding.requestPrimaryAction.show()
            binding.requestSecondaryAction.show()

            binding.requestPrimaryAction.setOnClickListener {
                binding.requestProgress.show()
                binding.requestPrimaryAction.disappear()

                postInviteListener.onPostInviteAccept(postInvite) {
                    binding.requestProgress.hide()
                    binding.requestPrimaryAction.show()
                }

            }

            binding.requestSecondaryAction.setOnClickListener {
                postInviteListener.onPostInviteCancel(postInvite)
            }

            postInviteListener.onCheckForStaleData(postInvite)

        }

    }

    override fun onBindViewHolder(holder: PostInviteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostInviteViewHolder {
        return PostInviteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false))
    }


}